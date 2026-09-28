package app.termosh.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TotpGeneratorTest {

    private val gen = TotpGenerator()

    @Test
    fun `base32 decodes standard string`() {
        // "Hello!" -> base32 = JBSWY3DPEE======
        val bytes = gen.base32Decode("JBSWY3DPEE")
        assertEquals("Hello!", String(bytes))
    }

    @Test
    fun `base32 handles lowercase and spaces`() {
        val a = gen.base32Decode("JBSWY3DPEE")
        val b = gen.base32Decode("jbs wy3d pee")
        assertTrue(a.contentEquals(b))
    }

    @Test
    fun `base32 rejects invalid char`() {
        try {
            gen.base32Decode("1!!!!")
            throw AssertionError("Should have thrown")
        } catch (e: IllegalArgumentException) {
            // ok
        }
    }

    @Test
    fun `generates 6 digit code by default`() {
        val secret = gen.base32Decode("JBSWY3DPEB3W64TMMQ")
        val code = gen.generate(secret, timestampMs = 1_500_000_000_000L, digits = 6)
        assertEquals(6, code.length)
        assertTrue(code.all { it.isDigit() })
    }

    @Test
    fun `code changes with time`() {
        val secret = gen.base32Decode("JBSWY3DPEB3W64TMMQ")
        val a = gen.generate(secret, timestampMs = 1_500_000_000_000L)
        val b = gen.generate(secret, timestampMs = 1_500_000_045_000L) // +45s
        assertNotEquals(a, b)
    }

    @Test
    fun `same time window yields same code`() {
        val secret = gen.base32Decode("JBSWY3DPEB3W64TMMQ")
        // обе метки в одном 30-секундном окне (10 и 20 секунд)
        val a = gen.generate(secret, timestampMs = 1_500_000_010_000L)
        val b = gen.generate(secret, timestampMs = 1_500_000_020_000L)
        assertEquals(a, b)
    }

    @Test
    fun `secondsRemaining in valid range`() {
        val s = gen.secondsRemaining(30)
        assertTrue(s in 1..30)
    }

    @Test
    fun `SHA256 algorithm`() {
        val secret = gen.base32Decode("JBSWY3DPEB3W64TMMQ")
        val code = gen.generate(secret, timestampMs = 1_500_000_000_000L, algorithm = "SHA256")
        assertEquals(6, code.length)
    }

    @Test
    fun `SHA512 algorithm`() {
        val secret = gen.base32Decode("JBSWY3DPEB3W64TMMQ")
        val code = gen.generate(secret, timestampMs = 1_500_000_000_000L, algorithm = "SHA512")
        assertEquals(6, code.length)
    }

    @Test
    fun `different algorithm yields different code`() {
        val secret = gen.base32Decode("JBSWY3DPEB3W64TMMQ")
        val a = gen.generate(secret, timestampMs = 1_500_000_000_000L, algorithm = "SHA1")
        val b = gen.generate(secret, timestampMs = 1_500_000_000_000L, algorithm = "SHA256")
        assertNotEquals(a, b)
    }

    @Test
    fun `8 digit code`() {
        val secret = gen.base32Decode("JBSWY3DPEB3W64TMMQ")
        val code = gen.generate(secret, timestampMs = 1_500_000_000_000L, digits = 8)
        assertEquals(8, code.length)
    }
}
