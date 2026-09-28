package app.termosh.feature.servers.imports

import app.termosh.core.ssh.imports.ConnectBotXmlParser
import app.termosh.core.ssh.imports.KnownHostsParser
import app.termosh.core.ssh.imports.IncludeExpander
import app.termosh.core.ssh.imports.IncludeReader
import app.termosh.core.ssh.imports.OpenSshConfigParser
import app.termosh.domain.model.KnownHost
import app.termosh.domain.model.KnownHostHashed
import app.termosh.domain.model.Server
import app.termosh.domain.model.ServerAuth
import app.termosh.domain.model.SshKey
import app.termosh.domain.model.SshKeyType
import app.termosh.domain.model.SecretRef
import app.termosh.domain.repository.KnownHostRepository
import app.termosh.domain.repository.SecretCodec
import app.termosh.domain.repository.ServerRepository
import app.termosh.domain.repository.SshKeyRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import java.security.MessageDigest

data class ImportResult(
    val serversImported: Int,
    val knownHostsImported: Int,
    val warnings: List<String>,
)

@Singleton
class ConfigImporter @Inject constructor(
    private val serverRepo: ServerRepository,
    private val knownHostRepo: KnownHostRepository,
    private val codec: SecretCodec,
    private val keyRepo: SshKeyRepository,
) {

    suspend fun importSshConfig(
        text: String,
        includeReader: IncludeReader? = null,
        basePath: String? = null,
    ): ImportResult {
        val warnings = mutableListOf<String>()
        val effectiveText = if (includeReader != null && basePath != null) {
            val res = IncludeExpander.expand(text, basePath, includeReader)
            warnings += res.warnings
            res.text
        } else {
            val hasInclude = text.lineSequence().any {
                it.trim().lowercase().startsWith("include") 
            }
            if (hasInclude) warnings += "В файле есть Include, но исходный путь не передан — директивы пропущены"
            text
        }
        val hosts = OpenSshConfigParser.parse(effectiveText)
        var count = 0

        hosts.forEach { h ->
            if (h.hostPatterns.any { it.contains('*') || it.contains('?') }) {
                warnings += "Пропущен wildcard host: ${h.hostPatterns.joinToString(",")}"
                return@forEach
            }
            val hostName = h.hostName ?: h.hostPatterns.firstOrNull() ?: return@forEach
            val username = h.user ?: "root"
            val port = h.port ?: 22

            // Пароль не знаем — вписываем пустой SecretRef, пользователь потом введёт
            val emptySecret = codec.encryptString("")

            val server = Server(
                id = UUID.randomUUID().toString(),
                name = h.hostPatterns.firstOrNull() ?: hostName,
                host = hostName,
                port = port,
                username = username,
                auth = ServerAuth.Password(emptySecret),
                proxyJumpId = null,
                tags = emptyList(),
                createdAt = System.currentTimeMillis(),
                lastUsedAt = null,
                useMosh = false,
                startupCommands = emptyList(),
                envVars = emptyMap(),
                envSecrets = emptyMap(),
            )
            serverRepo.save(server)
            count++

            if (h.identityFiles.isNotEmpty()) {
                warnings += "${server.name}: указан IdentityFile (${h.identityFiles.size} шт) — импортируйте ключ вручную"
            }
            if (h.proxyJump != null) {
                warnings += "${server.name}: ProxyJump=${h.proxyJump} — настройте вручную"
            }
        }

        return ImportResult(count, 0, warnings)
    }

    suspend fun importKnownHosts(text: String): ImportResult {
        val entries = KnownHostsParser.parse(text)
        val warnings = mutableListOf<String>()
        var plain = 0
        var hashed = 0
        entries.forEach { e ->
            when (e) {
                is KnownHostsParser.Entry.Plain -> {
                    knownHostRepo.save(
                        KnownHost(
                            host = e.host,
                            port = e.port,
                            fingerprintSha256 = e.fingerprintSha256,
                            addedAt = System.currentTimeMillis(),
                        ),
                    )
                    plain++
                }
                is KnownHostsParser.Entry.Hashed -> {
                    knownHostRepo.saveHashed(
                        KnownHostHashed(
                            salt = e.saltB64,
                            hash = e.hashB64,
                            keyType = e.keyType,
                            keyBase64 = e.keyBase64,
                            fingerprintSha256 = e.fingerprintSha256,
                            addedAt = System.currentTimeMillis(),
                        ),
                    )
                    hashed++
                }
            }
        }
        if (hashed > 0) {
            warnings += "Хешированных known_hosts: $hashed. Импортированы, будут проверяться при подключении."
        }
        return ImportResult(0, plain + hashed, warnings)
    }

    suspend fun importConnectBot(xml: String): ImportResult {
        val warnings = mutableListOf<String>()
        val parsed = ConnectBotXmlParser.parse(xml)

        // 1) ключи
        val idMap = mutableMapOf<String, String>()
        var keysImported = 0
        parsed.pubkeys.forEach { pk ->
            val priv = pk.privateKeyOpenSsh
            val pub = pk.publicKeyOpenSsh ?: ""
            if (priv.isNullOrBlank()) {
                warnings += "ConnectBot ключ ${pk.nickname ?: pk.id}: приватной части нет — пропущен"
                return@forEach
            }
            val newId = UUID.randomUUID().toString()
            val ref = codec.encrypt(priv.toByteArray(Charsets.UTF_8))
            val fp = runCatching {
                val digest = MessageDigest.getInstance("SHA-256").digest(priv.toByteArray(Charsets.UTF_8))
                "SHA256:" + java.util.Base64.getEncoder().withoutPadding().encodeToString(digest)
            }.getOrDefault("")
            val type = runCatching {
                if (pub.contains("ssh-ed25519")) SshKeyType.ED25519
                else if (pub.contains("ssh-rsa")) SshKeyType.RSA
                else if (pub.contains("ecdsa-")) SshKeyType.ECDSA_P256
                else SshKeyType.ED25519
            }.getOrDefault(SshKeyType.ED25519)

            keyRepo.save(
                SshKey(
                    id = newId,
                    type = type,
                    privateKeyRef = ref,
                    publicKeyOpenSsh = pub,
                    fingerprintSha256 = fp,
                    comment = pk.nickname,
                    createdAt = System.currentTimeMillis(),
                )
            )
            idMap[pk.id] = newId
            keysImported++
        }

        // 2) хосты
        var count = 0
        parsed.hosts.forEach { h ->
            val pid: String? = h.pubkeyId
            val pwd: String? = h.password
            val auth: ServerAuth = when {
                h.usePubkey && pid != null && idMap.containsKey(pid) ->
                    ServerAuth.Keys(listOf(idMap.getValue(pid)))
                pwd != null -> ServerAuth.Password(codec.encryptString(pwd))
                else -> ServerAuth.Password(codec.encryptString(""))
            }
            if (h.usePubkey && (pid == null || !idMap.containsKey(pid))) {
                warnings += "${h.nickname.ifBlank { h.hostname }}: pubkey не найден — импортирован без ключа"
            }
            if (pwd == null && !h.usePubkey) {
                warnings += "${h.nickname.ifBlank { h.hostname }}: пароль не найден — введите при первом подключении"
            }

            serverRepo.save(
                Server(
                    id = UUID.randomUUID().toString(),
                    name = h.nickname.ifBlank { h.hostname },
                    host = h.hostname,
                    port = h.port,
                    username = h.username,
                    auth = auth,
                    proxyJumpId = null,
                    tags = emptyList(),
                    createdAt = System.currentTimeMillis(),
                    lastUsedAt = null,
                    useMosh = false,
                    startupCommands = emptyList(),
                    envVars = emptyMap(),
                    envSecrets = emptyMap(),
                )
            )
            count++
        }

        if (count == 0) warnings += "В XML не найдено ни одного хоста"
        return ImportResult(count, 0, warnings)
    }
}
