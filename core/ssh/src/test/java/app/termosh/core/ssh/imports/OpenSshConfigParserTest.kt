package app.termosh.core.ssh.imports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenSshConfigParserTest {

    @Test
    fun `parses simple host`() {
        val cfg = """
            Host prod
                HostName 1.2.3.4
                User root
                Port 2222
        """.trimIndent()
        val hosts = OpenSshConfigParser.parse(cfg)
        assertEquals(1, hosts.size)
        val h = hosts[0]
        assertEquals(listOf("prod"), h.hostPatterns)
        assertEquals("1.2.3.4", h.hostName)
        assertEquals("root", h.user)
        assertEquals(2222, h.port)
    }

    @Test
    fun `parses multiple hosts`() {
        val cfg = """
            Host a
                HostName a.example.com
            Host b
                HostName b.example.com
                Port 22
        """.trimIndent()
        val hosts = OpenSshConfigParser.parse(cfg)
        assertEquals(2, hosts.size)
        assertEquals("a.example.com", hosts[0].hostName)
        assertEquals("b.example.com", hosts[1].hostName)
    }

    @Test
    fun `strips comments`() {
        val cfg = """
            # comment
            Host prod
                HostName 1.2.3.4  # inline comment
                User root
        """.trimIndent()
        val hosts = OpenSshConfigParser.parse(cfg)
        assertEquals("1.2.3.4", hosts[0].hostName)
    }

    @Test
    fun `collects identity files`() {
        val cfg = """
            Host prod
                HostName 1.2.3.4
                IdentityFile ~/.ssh/id_ed25519
                IdentityFile ~/.ssh/id_rsa
        """.trimIndent()
        val hosts = OpenSshConfigParser.parse(cfg)
        assertEquals(2, hosts[0].identityFiles.size)
    }

    @Test
    fun `empty input yields empty list`() {
        assertTrue(OpenSshConfigParser.parse("").isEmpty())
        assertTrue(OpenSshConfigParser.parse("# just comment").isEmpty())
    }
}
