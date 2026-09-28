package app.termosh.core.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeystoreManager @Inject constructor() {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "termosh.master.aes"
        private const val KEY_SIZE_BITS = 256
    }

    private fun loadKeyStore(): KeyStore =
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    fun getOrCreateMasterKey(): SecretKey {
        val ks = loadKeyStore()
        val existing = ks.getKey(MASTER_KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        return generateMasterKey()
    }

    private fun generateMasterKey(): SecretKey {
        return try {
            generateKeyWithStrongBox()
        } catch (t: Throwable) {
            runCatching {
                val ks = loadKeyStore()
                if (ks.containsAlias(MASTER_KEY_ALIAS)) {
                    ks.deleteEntry(MASTER_KEY_ALIAS)
                }
            }
            generateKeyWithoutStrongBox()
        }
    }

    private fun generateKeyWithStrongBox(): SecretKey {
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER,
        )
        val builder = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setUserAuthenticationRequired(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }

        generator.init(builder.build())
        return generator.generateKey()
    }

    private fun generateKeyWithoutStrongBox(): SecretKey {
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER,
        )
        val spec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setUserAuthenticationRequired(false)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    fun deleteMasterKey() {
        val ks = loadKeyStore()
        if (ks.containsAlias(MASTER_KEY_ALIAS)) {
            ks.deleteEntry(MASTER_KEY_ALIAS)
        }
    }

    fun hasMasterKey(): Boolean = loadKeyStore().containsAlias(MASTER_KEY_ALIAS)
}
