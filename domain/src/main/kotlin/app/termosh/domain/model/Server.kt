package app.termosh.domain.model

/**
 * Профиль сервера в домене.
 *
 * Отличается от ServerEntity: без IV/ciphertext, без sortOrder,
 * но с уже расшифрованным (на момент использования) секретом.
 * Секрет представлен как EncryptedSecret — репозиторий решает,
 * когда расшифровывать.
 */
data class Server(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val auth: ServerAuth,
    val proxyJumpId: String?,
    val tags: List<String>,
    val createdAt: Long,
    val useMosh: Boolean = false,
    val useTmux: Boolean = false,
    val totpSecretId: String? = null,
    val useJumpCredentials: Boolean = false,
    val startupCommands: List<String> = emptyList(),
    val envVars: Map<String, String> = emptyMap(),
    val envSecrets: Map<String, SecretRef> = emptyMap(),
    val lastUsedAt: Long?,
)

sealed interface ServerAuth {

    /** Пароль хранится зашифрованным в БД. */
    data class Password(val secretRef: SecretRef) : ServerAuth

    /** Список ключей в порядке приоритета (первый — самый приоритетный). */
    data class Keys(val keyIds: List<String>) : ServerAuth {
        val primaryKeyId: String? get() = keyIds.firstOrNull()
    }
}
