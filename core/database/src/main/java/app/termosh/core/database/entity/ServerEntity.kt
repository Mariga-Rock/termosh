package app.termosh.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Профиль сервера.
 *
 * Пароль (если используется) хранится зашифрованным через CryptoManager:
 * IV отдельно, ciphertext отдельно. При authType = KEY эти колонки null,
 * а ключ берётся из SshKeyEntity по keyId.
 */
@Entity(
    tableName = "servers",
    indices = [
        Index("host", "port", unique = false),
    ],
)
data class ServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val authType: AuthType,

    // Пароль (если authType = PASSWORD)
    val passwordIv: ByteArray? = null,
    val passwordCiphertext: ByteArray? = null,

    // Ключ (если authType = KEY)
    val keyIdsJson: String = "[]",

    // ProxyJump (ссылка на другой сервер той же таблицы)
    val proxyJumpId: String? = null,

    // Теги через запятую
    val tags: String = "",

    val createdAt: Long,
    val lastUsedAt: Long? = null,
    val sortOrder: Int = 0,
    val useMosh: Boolean = false,
    val totpSecretId: String? = null,
    val useJumpCredentials: Boolean = false,
    val startupCommandsJson: String = "[]",
    val envVarsJson: String = "{}",
    val envSecretsJson: String = "{}",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ServerEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

enum class AuthType {
    PASSWORD,
    KEY,
}
