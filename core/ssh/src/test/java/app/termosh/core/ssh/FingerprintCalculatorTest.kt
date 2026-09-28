package app.termosh.core.ssh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator

class FingerprintCalculatorTest {

    private val calc = FingerprintCalculator()

    private fun rsaPair() =
        KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    @Test
    fun `fingerprint has SHA256 prefix`() {
        val fp = calc.sha256(rsaPair().public)
        assertTrue(fp.startsWith("SHA256:"))
    }

    @Test
    fun `same key same fingerprint`() {
        val pair = rsaPair()
        assertEquals(calc.sha256(pair.public), calc.sha256(pair.public))
    }

    @Test
    fun `different keys different fingerprints`() {
        val a = calc.sha256(rsaPair().public)
        val b = calc.sha256(rsaPair().public)
        assertNotEquals(a, b)
    }

    @Test
    fun `fingerprint is base64 without padding`() {
        val fp = calc.sha256(rsaPair().public)
        val b64 = fp.substringAfter("SHA256:")
        assertTrue(!b64.contains("="))
    }
}
