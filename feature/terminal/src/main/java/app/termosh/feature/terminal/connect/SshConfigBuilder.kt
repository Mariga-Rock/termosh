package app.termosh.feature.terminal.connect

import app.termosh.core.ssh.model.ProxyJumpConfig
import app.termosh.core.ssh.model.SshAuthMethod
import app.termosh.core.ssh.model.SshConnectionConfig
import app.termosh.domain.model.Server
import app.termosh.domain.model.ServerAuth
import app.termosh.domain.repository.SecretCodec
import app.termosh.domain.repository.SshKeyRepository
import app.termosh.domain.usecase.server.GetServerUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Строит конфигурацию SSH-подключения из доменной модели Server.
 * Держит в себе разбор auth и ProxyJump, чтобы ViewModel не знала о деталях.
 */
@Singleton
class SshConfigBuilder @Inject constructor(
    private val getServer: GetServerUseCase,
    private val codec: SecretCodec,
    private val keyRepository: SshKeyRepository,
) {

    suspend fun authOf(server: Server): SshAuthMethod = when (val a = server.auth) {
        is ServerAuth.Password -> SshAuthMethod.Password(codec.decryptString(a.secretRef).toCharArray())
        is ServerAuth.Keys -> {
            val keys = keyRepository.getMany(a.keyIds)
            if (keys.isEmpty()) error("Ни один SSH-ключ не найден: ${a.keyIds}")
            SshAuthMethod.PublicKeys(
                keys = keys.map { k ->
                    SshAuthMethod.KeyMaterial(
                        privateKeyPkcs8 = codec.decrypt(k.privateKeyRef),
                        publicKeyOpenSsh = k.publicKeyOpenSsh,
                    )
                },
            )
        }
    }

    suspend fun buildConfig(server: Server): SshConnectionConfig {
        val jumpId = server.proxyJumpId
        val jump = if (jumpId != null) {
            val j = getServer(jumpId)
            ProxyJumpConfig(host = j.host, port = j.port, username = j.username, auth = authOf(j))
        } else null
        return SshConnectionConfig(
            host = server.host,
            port = server.port,
            username = server.username,
            proxyJump = jump,
            useJumpCredentialsForTarget = server.useJumpCredentials,
        )
    }

    suspend fun resolveTotpSecret(server: Server, totpRepository: app.termosh.domain.repository.TotpRepository): ByteArray? {
        val id = server.totpSecretId ?: return null
        return runCatching {
            val s = totpRepository.getById(id)
            if (s != null) codec.decrypt(s.secretRef) else null
        }.getOrNull()
    }
}
