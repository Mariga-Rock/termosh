package app.termosh.domain.model

/**
 * Ссылка на секрет, хранящийся в БД в зашифрованном виде.
 * Домен не знает, чем именно зашифровано — это задача инфраструктуры.
 *
 * Позволяет не таскать plaintext-пароли через весь пайплайн.
 */
data class SecretRef(
    val iv: ByteArray,
    val ciphertext: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SecretRef) return false
        return iv.contentEquals(other.iv) && ciphertext.contentEquals(other.ciphertext)
    }

    override fun hashCode(): Int = 31 * iv.contentHashCode() + ciphertext.contentHashCode()
}
