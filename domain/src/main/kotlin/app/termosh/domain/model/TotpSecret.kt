package app.termosh.domain.model

data class TotpSecret(
    val id: String,
    val label: String,
    val issuer: String,
    val secretRef: SecretRef,
    val digits: Int = 6,
    val periodSec: Int = 30,
    val algorithm: String = "SHA1",
    val createdAt: Long,
)
