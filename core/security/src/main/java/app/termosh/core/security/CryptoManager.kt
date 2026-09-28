package app.termosh.core.security

import app.termosh.core.security.model.EncryptedData
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor(
    private val keystoreManager: KeystoreManager,
) {

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
    }

    fun encrypt(plaintext: ByteArray): EncryptedData {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreManager.getOrCreateMasterKey())
        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedData(iv = cipher.iv, ciphertext = ciphertext)
    }

    fun decrypt(data: EncryptedData): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, data.iv)
        cipher.init(Cipher.DECRYPT_MODE, keystoreManager.getOrCreateMasterKey(), spec)
        return cipher.doFinal(data.ciphertext)
    }

    fun encryptString(plaintext: String): EncryptedData =
        encrypt(plaintext.toByteArray(Charsets.UTF_8))

    fun decryptString(data: EncryptedData): String =
        decrypt(data).toString(Charsets.UTF_8)

    fun canDecrypt(data: EncryptedData): Boolean = try {
        decrypt(data)
        true
    } catch (_: GeneralSecurityException) {
        false
    }
}
