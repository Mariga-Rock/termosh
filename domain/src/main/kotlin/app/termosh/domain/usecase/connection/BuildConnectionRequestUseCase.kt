package app.termosh.domain.usecase.connection

import app.termosh.domain.model.ConnectionAuth
import app.termosh.domain.model.ConnectionRequest
import app.termosh.domain.model.DomainError
import app.termosh.domain.model.ServerAuth
import app.termosh.domain.repository.SecretCodec
import app.termosh.domain.repository.ServerRepository
import app.termosh.domain.repository.SshKeyRepository

class BuildConnectionRequestUseCase(
    private val serverRepository: ServerRepository,
    private val keyRepository: SshKeyRepository,
    private val codec: SecretCodec,
) {
    suspend operator fun invoke(serverId: String): ConnectionRequest {
        val server = serverRepository.getById(serverId)
            ?: throw DomainError.NotFound("server:$serverId")

        val auth: ConnectionAuth = when (val a = server.auth) {
            is ServerAuth.Password -> {
                val plain = codec.decryptString(a.secretRef)
                ConnectionAuth.Password(plain.toCharArray())
            }
            is ServerAuth.Keys -> {
                val keys = keyRepository.getMany(a.keyIds)
                if (keys.isEmpty()) throw DomainError.NotFound("keys:${a.keyIds}")
                val first = keys.first()
                ConnectionAuth.PrivateKey(
                    privateKeyPkcs8 = codec.decrypt(first.privateKeyRef),
                    publicKeyOpenSsh = first.publicKeyOpenSsh,
                )
            }
        }

        return ConnectionRequest(
            serverId = server.id,
            host = server.host,
            port = server.port,
            username = server.username,
            auth = auth,
        )
    }
}
