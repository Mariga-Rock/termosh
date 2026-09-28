package app.termosh.domain.model

/**
 * Запрос на подключение. Формируется из Server + способа аутентификации.
 * Отдаётся в :core:ssh, где превращается в SshConnectionConfig.
 */
data class ConnectionRequest(
    val serverId: String,
    val host: String,
    val port: Int,
    val username: String,
    val auth: ConnectionAuth,
    val keepAliveIntervalSec: Int = 30,
    val connectTimeoutMs: Int = 15_000,
)

sealed interface ConnectionAuth {

    data class Password(val password: CharArray) : ConnectionAuth {
        override fun equals(other: Any?): Boolean =
            other is Password && password.contentEquals(other.password)

        override fun hashCode(): Int = password.contentHashCode()
    }

    data class PrivateKey(
        val privateKeyPkcs8: ByteArray,
        val publicKeyOpenSsh: String,
    ) : ConnectionAuth {
        override fun equals(other: Any?): Boolean =
            other is PrivateKey &&
                privateKeyPkcs8.contentEquals(other.privateKeyPkcs8) &&
                publicKeyOpenSsh == other.publicKeyOpenSsh

        override fun hashCode(): Int =
            31 * privateKeyPkcs8.contentHashCode() + publicKeyOpenSsh.hashCode()
    }
}
