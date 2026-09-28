package app.termosh.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellEscapeTest {

    @Test
    fun `simple value quoted`() {
        assertEquals("'hello'", ShellEscape.quote("hello"))
    }

    @Test
    fun `empty value quoted`() {
        assertEquals("''", ShellEscape.quote(""))
    }

    @Test
    fun `single quote wrapped safely`() {
        val r = ShellEscape.quote("it's")
        assertTrue(r.startsWith("'"))
        assertTrue(r.endsWith("'"))
        assertTrue(r.length >= 8)
    }

    @Test
    fun `spaces preserved`() {
        assertEquals("'a b c'", ShellEscape.quote("a b c"))
    }

    @Test
    fun `dollar sign not expanded`() {
        val input = "\$HOME"
        val expected = "'\$HOME'"
        assertEquals(expected, ShellEscape.quote(input))
    }

    @Test
    fun `backslash preserved`() {
        val input = "a\\b"
        val r = ShellEscape.quote(input)
        assertTrue(r.startsWith("'"))
        assertTrue(r.endsWith("'"))
    }
}
