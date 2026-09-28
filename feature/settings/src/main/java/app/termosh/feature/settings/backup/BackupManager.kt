package app.termosh.feature.settings.backup

import app.termosh.core.common.AuditLog
import app.termosh.domain.model.KnownHost
import app.termosh.domain.model.PortForward
import app.termosh.domain.model.Server
import app.termosh.domain.model.ServerAuth
import app.termosh.domain.model.Snippet
import app.termosh.domain.model.SshKey
import app.termosh.domain.model.SshKeyType
import app.termosh.domain.repository.KnownHostRepository
import app.termosh.domain.repository.PortForwardRepository
import app.termosh.domain.repository.SecretCodec
import app.termosh.domain.repository.ServerRepository
import app.termosh.domain.repository.SnippetRepository
import app.termosh.domain.repository.SshKeyRepository
import kotlinx.coroutines.flow.first
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class BackupManager @Inject constructor(
    private val servers: ServerRepository,
    private val keys: SshKeyRepository,
    private val snippets: SnippetRepository,
    private val knownHosts: KnownHostRepository,
    private val portForwards: PortForwardRepository,
    private val codec: SecretCodec,    @ApplicationContext private val context: Context,

) {

    @Suppress("DEPRECATION")
    private val appVersion: String by lazy {
        runCatching {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "unknown"
        }.getOrDefault("unknown")
    }


    /**
     * @param includeSecrets true — полный бэкап с паролями и ключами (шифруется)
     *                       false — конфиг без секретов (открытый JSON)
     */
    suspend fun export(includeSecrets: Boolean): BackupData {
        val serverList = servers.observeAll().first()
        val keyList = keys.observeAll().first()
        val snippetList = snippets.observeAll().first()

        val backupKeys = keyList.map { k ->
            val pkcs8Base64 = if (includeSecrets) {
                runCatching {
                    Base64.getEncoder().encodeToString(codec.decrypt(k.privateKeyRef))
                }.getOrNull()
            } else null
            BackupKey(
                id = k.id,
                type = k.type.name,
                privateKeyPkcs8Base64 = pkcs8Base64,
                publicKeyOpenSsh = k.publicKeyOpenSsh,
                fingerprintSha256 = k.fingerprintSha256,
                comment = k.comment,
                createdAt = k.createdAt,
            )
        }

        val backupServers = serverList.map { s ->
            val authType = when (s.auth) {
                is ServerAuth.Password -> "PASSWORD"
                is ServerAuth.Keys -> "KEY"
            }
            val passwordPlain = if (includeSecrets) {
                (s.auth as? ServerAuth.Password)?.let {
                    runCatching { codec.decryptString(it.secretRef) }.getOrNull()
                }
            } else null
            val keyIds = (s.auth as? ServerAuth.Keys)?.keyIds

            val envSecretsPlain = if (includeSecrets) {
                s.envSecrets.mapNotNull { (k, ref) ->
                    runCatching { k to codec.decryptString(ref) }.getOrNull()
                }.toMap()
            } else emptyMap()

            BackupServer(
                id = s.id,
                name = s.name,
                host = s.host,
                port = s.port,
                username = s.username,
                tags = s.tags,
                authType = authType,
                passwordPlain = passwordPlain,
                keyIds = keyIds,
                proxyJumpId = s.proxyJumpId,
                useMosh = s.useMosh,
                totpSecretId = s.totpSecretId,
                startupCommands = s.startupCommands,
                envVars = s.envVars,
                envSecretsPlain = envSecretsPlain,
                createdAt = s.createdAt,
                lastUsedAt = s.lastUsedAt,
            )
        }

        val backupSnippets = snippetList.map {
            BackupSnippet(it.id, it.name, it.command, it.sortOrder)
        }

        val backupHosts = mutableListOf<BackupKnownHost>()
        val backupFwds = mutableListOf<BackupPortForward>()

        AuditLog.log(
            event = if (includeSecrets) "backup_export_vault" else "backup_export_config",
            fields = mapOf(
                "servers" to backupServers.size,
                "keys" to backupKeys.size,
                "snippets" to backupSnippets.size,
                "includesSecrets" to includeSecrets,
            ),
        )

        return BackupData(
            format = if (includeSecrets) "termoshvault" else "termosh",
            formatVersion = CURRENT_FORMAT_VERSION,
            appVersion = appVersion,
            exportedAt = System.currentTimeMillis(),
            includesSecrets = includeSecrets,
            servers = backupServers,
            keys = backupKeys,
            snippets = backupSnippets,
            knownHosts = backupHosts,
            portForwards = backupFwds,
        )
    }

    /**
     * @param secretsAvailable true, если файл содержал секреты и они были расшифрованы.
     *                         Если false — серверы/ключи импортируются без паролей и приватных ключей.
     */
    suspend fun import(data: BackupData, secretsAvailable: Boolean): ImportStats {
        var keysImported = 0
        var keysSkipped = 0
        data.keys.forEach { k ->
            if (k.privateKeyPkcs8Base64 == null) {
                keysSkipped++
                return@forEach
            }
            val pkcs8 = Base64.getDecoder().decode(k.privateKeyPkcs8Base64)
            val ref = codec.encrypt(pkcs8)
            keys.save(
                SshKey(
                    id = k.id,
                    type = SshKeyType.valueOf(k.type),
                    privateKeyRef = ref,
                    publicKeyOpenSsh = k.publicKeyOpenSsh,
                    fingerprintSha256 = k.fingerprintSha256,
                    comment = k.comment,
                    createdAt = k.createdAt,
                )
            )
            keysImported++
        }

        var serversImported = 0
        var serversWithoutAuth = 0
        data.servers.forEach { s ->
            val auth: ServerAuth = when (s.authType) {
                "PASSWORD" -> {
                    if (s.passwordPlain != null) {
                        ServerAuth.Password(codec.encryptString(s.passwordPlain))
                    } else {
                        serversWithoutAuth++
                        ServerAuth.Password(codec.encryptString(""))
                    }
                }
                "KEY" -> {
                    val keyIds = s.keyIds
                    if (keyIds.isNullOrEmpty()) {
                        serversWithoutAuth++
                        ServerAuth.Keys(emptyList())
                    } else {
                        ServerAuth.Keys(keyIds)
                    }
                }
                else -> {
                    serversWithoutAuth++
                    ServerAuth.Password(codec.encryptString(""))
                }
            }

            val envSecrets = s.envSecretsPlain.mapValues { (_, v) -> codec.encryptString(v) }

            servers.save(
                Server(
                    id = s.id,
                    name = s.name,
                    host = s.host,
                    port = s.port,
                    username = s.username,
                    auth = auth,
                    proxyJumpId = s.proxyJumpId,
                    tags = s.tags,
                    createdAt = if (s.createdAt > 0) s.createdAt else System.currentTimeMillis(),
                    lastUsedAt = s.lastUsedAt,
                    useMosh = s.useMosh,
                    totpSecretId = s.totpSecretId,
                    startupCommands = s.startupCommands,
                    envVars = s.envVars,
                    envSecrets = envSecrets,
                )
            )
            serversImported++
        }

        var snippetsImported = 0
        data.snippets.forEach { sn ->
            snippets.save(
                Snippet(
                    id = sn.id,
                    name = sn.name,
                    command = sn.command,
                    sortOrder = sn.sortOrder,
                    createdAt = System.currentTimeMillis(),
                )
            )
            snippetsImported++
        }

        data.knownHosts.forEach { h ->
            knownHosts.save(
                KnownHost(
                    host = h.host,
                    port = h.port,
                    fingerprintSha256 = h.fingerprintSha256,
                    addedAt = h.addedAt,
                )
            )
        }

        data.portForwards.forEach { p ->
            portForwards.save(
                PortForward(
                    id = p.id,
                    serverId = p.serverId,
                    localPort = p.localPort,
                    remoteHost = p.remoteHost,
                    remotePort = p.remotePort,
                    enabled = p.enabled,
                    createdAt = p.createdAt,
                )
            )
        }

        AuditLog.log(
            event = "backup_import",
            fields = mapOf(
                "servers" to serversImported,
                "keys" to keysImported,
                "keysSkipped" to keysSkipped,
                "serversWithoutAuth" to serversWithoutAuth,
                "snippets" to snippetsImported,
                "secretsAvailable" to secretsAvailable,
            ),
        )

        return ImportStats(
            serversImported = serversImported,
            keysImported = keysImported,
            keysSkipped = keysSkipped,
            serversWithoutAuth = serversWithoutAuth,
            snippetsImported = snippetsImported,
            includesSecrets = data.includesSecrets,
        )
    }
}

data class ImportStats(
    val serversImported: Int,
    val keysImported: Int,
    val keysSkipped: Int,
    val serversWithoutAuth: Int,
    val snippetsImported: Int,
    val includesSecrets: Boolean,
)

@Suppress("unused")
private const val CURRENT_FORMAT_VERSION = 1
