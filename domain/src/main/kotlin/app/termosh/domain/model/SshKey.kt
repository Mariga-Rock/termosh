package app.termosh.domain.model

/**
 * SSH-ключ в домене.
 *
 * Приватная часть лежит зашифрованной (SecretRef). Публичная — открытым текстом,
 * она и так публичная.
 */
data class SshKey(
    val id: String,
    val type: SshKeyType,
    val privateKeyRef: SecretRef,
    val publicKeyOpenSsh: String,
    val fingerprintSha256: String,
    val comment: String?,
    val createdAt: Long,
)

enum class SshKeyType {
    ED25519,
    RSA,
    ECDSA_P256,
}
