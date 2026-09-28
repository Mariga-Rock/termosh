package app.termosh.feature.settings.backup

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {
    private const val MAGIC = "TERMOSH1"
    private const val ITER = 100_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    fun encrypt(json: String, password: CharArray): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val key = derive(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val ct = cipher.doFinal(json.toByteArray(Charsets.UTF_8))
        val b64 = Base64.getEncoder()
        return buildString {
            append(MAGIC).append('\n')
            append(b64.encodeToString(salt)).append('\n')
            append(b64.encodeToString(iv)).append('\n')
            append(b64.encodeToString(ct)).append('\n')
        }
    }

    fun decrypt(text: String, password: CharArray): String {
        val lines = text.trim().split('\n')
        require(lines.size >= 4 && lines[0] == MAGIC) { "Неверный формат .termosh" }
        val b64 = Base64.getDecoder()
        val salt = b64.decode(lines[1].trim())
        val iv = b64.decode(lines[2].trim())
        val ct = b64.decode(lines[3].trim())
        val key = derive(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(ct), Charsets.UTF_8)
    }

    private fun derive(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITER, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }
}
