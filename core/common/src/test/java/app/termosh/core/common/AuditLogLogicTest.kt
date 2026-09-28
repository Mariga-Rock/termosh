package app.termosh.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

/**
 * Логика hash-цепочки проверяется отдельно (без файловой системы).
 */
class AuditLogLogicTest {

    private fun sha256(s: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `same input produces same hash`() {
        assertEquals(sha256("abc"), sha256("abc"))
    }

    @Test
    fun `different input produces different hash`() {
        assertTrue(sha256("abc") != sha256("abd"))
    }

    @Test
    fun `hash chain detects tampering`() {
        // имитируем цепочку: h1 = sha("" + e1), h2 = sha(h1 + e2)
        val e1 = """{"event":"a"}"""
        val e2 = """{"event":"b"}"""
        val h1 = sha256("" + e1)
        val h2 = sha256(h1 + e2)

        // легитимно
        assertEquals(h2, sha256(h1 + e2))

        // подмена e1 — цепочка рассыпается
        val fakeE1 = """{"event":"X"}"""
        val fakeH1 = sha256("" + fakeE1)
        assertTrue(sha256(fakeH1 + e2) != h2)
    }
}
