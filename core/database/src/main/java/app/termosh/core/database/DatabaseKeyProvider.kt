package app.termosh.core.database

import android.content.Context
import app.termosh.core.security.CryptoManager
import app.termosh.core.security.model.EncryptedData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Поставщик парольной фразы для SQLCipher.
 *
 * Логика:
 *  1. При первом запуске генерируется случайные 32 байта.
 *  2. Шифруются мастер-ключом Keystore через CryptoManager.
 *  3. Зашифрованный blob пишется в filesDir/termosh.db.key.
 *  4. При последующих запусках blob читается и расшифровывается.
 *
 * Если мастер-ключ удалён или файл повреждён — база станет недоступна,
 * что ожидаемо: без ключа расшифровать её нельзя.
 */
@Singleton
class DatabaseKeyProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager,
) {

    companion object {
        private const val KEY_FILE = "termosh.db.key"
        private const val KEY_SIZE_BYTES = 32
    }

    @Volatile
    private var cached: ByteArray? = null

    /** Возвращает ключ БД, создавая его при первом обращении. */
    @Synchronized
    fun getOrCreateKey(): ByteArray {
        cached?.let { return it }
        val file = File(context.filesDir, KEY_FILE)
        val key = if (file.exists()) {
            val encrypted = deserialize(file.readBytes())
            cryptoManager.decrypt(encrypted)
        } else {
            val fresh = ByteArray(KEY_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
            val encrypted = cryptoManager.encrypt(fresh)
            file.writeBytes(serialize(encrypted))
            fresh
        }
        cached = key
        return key
    }

    /** Удаляет ключ. После этого БД станет недоступна. */
    @Synchronized
    fun wipe() {
        File(context.filesDir, KEY_FILE).delete()
        cached = null
    }

    // Формат: [ivLen:Int][iv][ciphertext]
    private fun serialize(data: EncryptedData): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val dos = java.io.DataOutputStream(out)
        dos.writeInt(data.iv.size)
        dos.write(data.iv)
        dos.write(data.ciphertext)
        dos.flush()
        return out.toByteArray()
    }

    private fun deserialize(bytes: ByteArray): EncryptedData {
        val dis = java.io.DataInputStream(java.io.ByteArrayInputStream(bytes))
        val ivLen = dis.readInt()
        require(ivLen in 1..32) { "Invalid IV length: $ivLen" }
        val iv = ByteArray(ivLen).also { dis.readFully(it) }
        val ciphertext = dis.readBytes()
        return EncryptedData(iv = iv, ciphertext = ciphertext)
    }
}
