package app.termosh.core.database.entity

import androidx.room.Entity

/**
 * Хешированная запись known_hosts в формате OpenSSH:
 *   |1|<base64-salt>|<base64-hmac-sha1(canonicalHost, salt)> <keytype> <key>
 * Имя хоста в открытом виде здесь не хранится — проверка идёт
 * через HMAC при попытке подключения к конкретному хосту.
 */
@Entity(
    tableName = "known_hosts_hashed",
    primaryKeys = ["salt", "hash", "keyType"],
)
data class KnownHostHashedEntity(
    val salt: String,             // base64
    val hash: String,             // base64
    val keyType: String,
    val keyBase64: String,
    val fingerprintSha256: String,
    val addedAt: Long,
)
