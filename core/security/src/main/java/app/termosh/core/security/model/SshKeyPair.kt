package app.termosh.core.security.model

data class SshKeyPair(
    val id: String,
    val type: SshKeyType,
    val encryptedPrivateKey: EncryptedData,
    val publicKeyOpenSsh: String,
    val fingerprintSha256: String,
    val comment: String?,
    val createdAt: Long,
)
