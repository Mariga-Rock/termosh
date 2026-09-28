package app.termosh.core.security.codec

import app.termosh.core.security.CryptoManager
import app.termosh.core.security.model.EncryptedData
import app.termosh.domain.model.SecretRef
import app.termosh.domain.repository.SecretCodec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoSecretCodec @Inject constructor(
    private val crypto: CryptoManager,
) : SecretCodec {

    override fun encrypt(plaintext: ByteArray): SecretRef {
        val ed = crypto.encrypt(plaintext)
        return SecretRef(iv = ed.iv, ciphertext = ed.ciphertext)
    }

    override fun decrypt(ref: SecretRef): ByteArray =
        crypto.decrypt(EncryptedData(iv = ref.iv, ciphertext = ref.ciphertext))
}
