package app.termosh.core.ssh.model

sealed interface SshAuthMethod {

    data class Password(
        val password: CharArray,
    ) : SshAuthMethod

    data class KeyMaterial(
        val privateKeyPkcs8: ByteArray,
        val publicKeyOpenSsh: String,
    )

    /**
     * Список ключей. Порядок — как у пользователя.
     * Перебираются последовательно до первого успеха.
     */
    data class PublicKeys(
        val keys: List<KeyMaterial>,
    ) : SshAuthMethod
}
