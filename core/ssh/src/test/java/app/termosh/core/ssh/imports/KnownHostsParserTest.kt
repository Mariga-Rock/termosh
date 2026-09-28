package app.termosh.core.ssh.imports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnownHostsParserTest {

    @Test
    fun `parses unhashed entry`() {
        val line = "example.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIBc3vDk8FvU="
        val result = KnownHostsParser.parse(line)
        assertEquals(1, result.size)
        assertEquals("example.com", result[0].host)
        assertTrue(result[0].fingerprintSha256.startsWith("SHA256:"))
    }

    @Test
    fun `skips hashed lines`() {
        val line = "|1|abcd|efgh ssh-rsa AAAAB3NzaC1yc2E="
        val result = KnownHostsParser.parse(line)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parses host with custom port`() {
        val line = "[example.com]:2222 ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIBc3vDk8FvU="
        val result = KnownHostsParser.parse(line)
        assertEquals("example.com", result[0].host)
    }
}
