package app.termosh.core.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextPatternsTest {

    @Test
    fun `matches https url`() {
        assertTrue(TextPatterns.URL.containsMatchIn("see https://example.com for details"))
    }

    @Test
    fun `matches http url`() {
        assertTrue(TextPatterns.URL.containsMatchIn("http://localhost:8080/api"))
    }

    @Test
    fun `matches ftp url`() {
        assertTrue(TextPatterns.URL.containsMatchIn("ftp://ftp.example.com/file"))
    }

    @Test
    fun `matches www url`() {
        assertTrue(TextPatterns.URL.containsMatchIn("visit www.example.com now"))
    }

    @Test
    fun `normalize adds https`() {
        assertEquals("https://www.example.com", TextPatterns.normalizeUrl("www.example.com"))
    }

    @Test
    fun `normalize keeps http`() {
        assertEquals("http://example.com", TextPatterns.normalizeUrl("http://example.com"))
    }

    @Test
    fun `ip matches simple`() {
        assertTrue(TextPatterns.IP.containsMatchIn("connect to 192.168.1.1"))
    }

    @Test
    fun `ip matches public`() {
        assertTrue(TextPatterns.IP.containsMatchIn("8.8.8.8"))
    }

    @Test
    fun `ip matches with port`() {
        assertTrue(TextPatterns.IP.containsMatchIn("server 10.0.0.1:22"))
    }

    @Test
    fun `ip does not match version`() {
        assertFalse(TextPatterns.IP.containsMatchIn("version 1.2"))
    }

    @Test
    fun `path matches absolute`() {
        assertTrue(TextPatterns.PATH.containsMatchIn("cd /var/log"))
    }

    @Test
    fun `path matches config`() {
        assertTrue(TextPatterns.PATH.containsMatchIn("error in /etc/ssh/sshd_config"))
    }

    @Test
    fun `extract url from line`() {
        val match = TextPatterns.URL.find("visit https://example.com/page")!!.value
        assertEquals("https://example.com/page", match)
    }
}
