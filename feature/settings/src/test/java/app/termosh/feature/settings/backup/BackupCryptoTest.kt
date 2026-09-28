package app.termosh.feature.settings.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {

    @Test
    fun `roundtrip with correct password`() {
        val data = """{"hello":"world","n":42}"""
        val pwd = "hunter2".toCharArray()
        val enc = BackupCrypto.encrypt(data, pwd)
        val dec = BackupCrypto.decrypt(enc, pwd)
        assertEquals(data, dec)
    }

    @Test
    fun `wrong password fails`() {
        val data = "secret data"
        val enc = BackupCrypto.encrypt(data, "correct".toCharArray())
        try {
            BackupCrypto.decrypt(enc, "wrong".toCharArray())
            throw AssertionError("Should have failed")
        } catch (t: Throwable) {
            // expected
        }
    }

    @Test
    fun `different calls produce different ciphertext`() {
        val data = "same"
        val pwd = "pwd".toCharArray()
        val a = BackupCrypto.encrypt(data, pwd)
        val b = BackupCrypto.encrypt(data, pwd)
        assertNotEquals(a, b) // разные IV
    }

    @Test
    fun `magic header present`() {
        val enc = BackupCrypto.encrypt("x", "p".toCharArray())
        assertTrue(enc.startsWith("TERMOSH1\n"))
    }

    @Test
    fun `invalid format rejected`() {
        try {
            BackupCrypto.decrypt("garbage", "p".toCharArray())
            throw AssertionError("Should have failed")
        } catch (t: Throwable) {
            // expected
        }
    }
}
