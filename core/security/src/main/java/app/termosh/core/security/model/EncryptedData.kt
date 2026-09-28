package app.termosh.core.security.model

data class EncryptedData(
    val iv: ByteArray,
    val ciphertext: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedData) return false
        return iv.contentEquals(other.iv) && ciphertext.contentEquals(other.ciphertext)
    }
    override fun hashCode(): Int = 31 * iv.contentHashCode() + ciphertext.contentHashCode()
}
