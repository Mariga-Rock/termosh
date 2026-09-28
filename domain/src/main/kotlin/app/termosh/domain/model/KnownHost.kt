package app.termosh.domain.model

/**
 * Отпечаток ключа хоста для TOFU-проверки.
 */
data class KnownHost(
    val host: String,
    val port: Int,
    val fingerprintSha256: String,
    val addedAt: Long,
)
