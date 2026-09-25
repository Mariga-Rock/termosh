package app.termosh.core.ssh.model

/**
 * Метод аутентификации.
 * Приватный ключ передаётся в PKCS#8 (расшифрованный из Keystore на лету).
 */
sealed interface SshAuthMethod {

    data class Password(
        val password: CharArray,
    ) : SshAuthMethod

    data class PublicKey(
        /** PKCS#8 encoded private key. */
        val privateKeyPkcs8: ByteArray,
        /** OpenSSH-строка публичного ключа. */
        val publicKeyOpenSsh: String,
    ) : SshAuthMethod
}
