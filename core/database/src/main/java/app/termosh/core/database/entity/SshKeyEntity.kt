package app.termosh.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * SSH-ключ. Приватная часть зашифрована мастер-ключом Keystore
 * (см. :core:security SshKeyStore), публичная хранится как OpenSSH-строка.
 */
@Entity(
    tableName = "ssh_keys",
    indices = [Index("fingerprintSha256", unique = true)],
)
data class SshKeyEntity(
    @PrimaryKey val id: String,
    val type: String, // SshKeyType.name
    val encryptedPrivateKeyIv: ByteArray,
    val encryptedPrivateKeyCiphertext: ByteArray,
    val publicKeyOpenSsh: String,
    val fingerprintSha256: String,
    val comment: String?,
    val createdAt: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SshKeyEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
