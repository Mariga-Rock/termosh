package app.termosh.core.ssh

import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator

class FingerprintCalculatorTest {

    private val calculator = FingerprintCalculator()

    @Test
    fun `fingerprint has SHA256 prefix`() {
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val fp = calculator.sha256(pair.public)
        assertTrue(fp.startsWith("SHA256:"))
    }
}
