package app.termosh.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "totp_secrets",
    indices = [Index("label", unique = true)],
)
data class TotpEntity(
    @PrimaryKey val id: String,
    val label: String,
    val issuer: String,
    val encryptedSecretIv: ByteArray,
    val encryptedSecretCiphertext: ByteArray,
    val digits: Int,
    val periodSec: Int,
    val algorithm: String,
    val createdAt: Long,
)
