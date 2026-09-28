package app.termosh.core.security

import app.termosh.core.security.model.SshKeyPair
import app.termosh.core.security.model.SshKeyType
import java.security.KeyPair
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshKeyStore @Inject constructor(
    private val cryptoManager: CryptoManager,
    private val generator: SshKeyPairGenerator,
) {

    fun generateAndWrap(type: SshKeyType, comment: String? = null): SshKeyPair {
        val keyPair = generator.generate(type)
        return wrap(keyPair, type, comment)
    }

    fun wrap(keyPair: KeyPair, type: SshKeyType, comment: String? = null): SshKeyPair {
        val privateBytes = keyPair.private.encoded
            ?: error("Private key has no PKCS#8 encoding")
        val encrypted = cryptoManager.encrypt(privateBytes)
        val publicOpenSsh = generator.toOpenSshPublicKey(keyPair, comment)
        val fingerprint = sha256Fingerprint(keyPair.public.encoded)

        return SshKeyPair(
            id = UUID.randomUUID().toString(),
            type = type,
            encryptedPrivateKey = encrypted,
            publicKeyOpenSsh = publicOpenSsh,
            fingerprintSha256 = fingerprint,
            comment = comment,
            createdAt = System.currentTimeMillis(),
        )
    }

    fun unwrapPrivateKeyBytes(stored: SshKeyPair): ByteArray =
        cryptoManager.decrypt(stored.encryptedPrivateKey)

    private fun sha256Fingerprint(encoded: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(encoded)
        val b64 = Base64.getEncoder().withoutPadding().encodeToString(digest)
        return "SHA256:$b64"
    }
}
