package app.termosh.core.licensing

import app.termosh.core.licensing.model.License
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom
import java.util.Base64

class LicenseFormatTest {

    private fun generateKeyPair(): Pair<Ed25519PrivateKeyParameters, Ed25519PublicKeyParameters> {
        val gen = Ed25519KeyPairGenerator()
        gen.init(Ed25519KeyGenerationParameters(SecureRandom()))
        val pair: AsymmetricCipherKeyPair = gen.generateKeyPair()
        return (pair.private as Ed25519PrivateKeyParameters) to
            (pair.public as Ed25519PublicKeyParameters)
    }

    @Test
    fun `canonical json contains all fields`() {
        val lic = License(
            version = 1,
            id = "lic-1",
            recipientName = "Ivan",
            devicePublicKeyBase64 = "AAAA",
            features = listOf("all"),
            issuedAt = 1000L,
            expiresAt = null,
            nonce = "abc",
        )
        val json = lic.encodeCanonical()
        val obj = JSONObject(json)
        assertEquals(1, obj.getInt("v"))
        assertEquals("lic-1", obj.getString("id"))
        assertEquals("Ivan", obj.getString("name"))
        assertTrue(obj.isNull("expiresAt"))
    }

    @Test
    fun `decode reverses encode`() {
        val lic = License(
            version = 1,
            id = "lic-2",
            recipientName = "Petr",
            devicePublicKeyBase64 = "BBBB",
            features = listOf("a", "b"),
            issuedAt = 2000L,
            expiresAt = 3000L,
            nonce = "xyz",
        )
        val back = License.decode(lic.encodeCanonical())
        assertEquals(lic.id, back.id)
        assertEquals(lic.recipientName, back.recipientName)
        assertEquals(lic.features, back.features)
        assertEquals(lic.expiresAt, back.expiresAt)
    }

    @Test
    fun `signature verifies with same key`() {
        val (priv, pub) = generateKeyPair()
        val payload = """{"v":1,"id":"x"}""".toByteArray()

        val signer = Ed25519Signer()
        signer.init(true, priv)
        signer.update(payload, 0, payload.size)
        val sig = signer.generateSignature()

        val verifier = Ed25519Signer()
        verifier.init(false, pub)
        verifier.update(payload, 0, payload.size)
        assertTrue(verifier.verifySignature(sig))
    }

    @Test
    fun `signature fails with different key`() {
        val (privA, _) = generateKeyPair()
        val (_, pubB) = generateKeyPair()
        val payload = "hello".toByteArray()

        val signer = Ed25519Signer()
        signer.init(true, privA)
        signer.update(payload, 0, payload.size)
        val sig = signer.generateSignature()

        val verifier = Ed25519Signer()
        verifier.init(false, pubB)
        verifier.update(payload, 0, payload.size)
        assertFalse(verifier.verifySignature(sig))
    }

    @Test
    fun `signature fails with tampered payload`() {
        val (priv, pub) = generateKeyPair()
        val payload = "original".toByteArray()
        val tampered = "modified".toByteArray()

        val signer = Ed25519Signer()
        signer.init(true, priv)
        signer.update(payload, 0, payload.size)
        val sig = signer.generateSignature()

        val verifier = Ed25519Signer()
        verifier.init(false, pub)
        verifier.update(tampered, 0, tampered.size)
        assertFalse(verifier.verifySignature(sig))
    }
}
