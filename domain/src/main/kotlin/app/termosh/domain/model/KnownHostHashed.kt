package app.termosh.domain.model

/**
 * Хешированная запись known_hosts (OpenSSH HashKnownHosts).
 * salt/hash — base64 без префикса "|1|".
 */
data class KnownHostHashed(
    val salt: String,
    val hash: String,
    val keyType: String,
    val keyBase64: String,
    val fingerprintSha256: String,
    val addedAt: Long,
)
