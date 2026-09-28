package app.termosh.core.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TermScreenTest {

    // --- базовое ---

    @Test
    fun `plain text`() {
        val s = TermScreen(rows = 5, cols = 20)
        s.write("hello")
        val rows = s.rowsSnapshot()
        assertEquals('h', rows[0][0].ch)
        assertEquals('o', rows[0][4].ch)
    }

    @Test
    fun `newline moves cursor`() {
        val s = TermScreen(rows = 5, cols = 20)
        s.write("one" + '\r' + '\n' + "two")
        val rows = s.rowsSnapshot()
        assertEquals('o', rows[0][0].ch)
        assertEquals('n', rows[0][1].ch)
        assertEquals('e', rows[0][2].ch)
        assertEquals('t', rows[1][0].ch)
        assertEquals('w', rows[1][1].ch)
        assertEquals('o', rows[1][2].ch)
    }

    @Test
    fun `carriage return resets column`() {
        val s = TermScreen(rows = 5, cols = 20)
        s.write("12345\rX")
        assertEquals('X', s.rowsSnapshot()[0][0].ch)
    }

    @Test
    fun `backspace`() {
        val s = TermScreen(rows = 5, cols = 20)
        s.write("ab\bc")
        val r = s.rowsSnapshot()[0]
        assertEquals('a', r[0].ch)
        assertEquals('c', r[1].ch)
    }

    @Test
    fun `tab advances to next 8`() {
        val s = TermScreen(rows = 5, cols = 20)
        s.write("a\tb")
        val r = s.rowsSnapshot()[0]
        assertEquals('a', r[0].ch)
        assertEquals('b', r[8].ch)
    }

    // --- SGR ---

    @Test
    fun `ansi red foreground`() {
        val s = TermScreen(rows = 5, cols = 20)
        s.write("\u001b[31mR")
        assertTrue(s.rowsSnapshot()[0][0].fg != null)
    }

    @Test
    fun `ansi reset clears style`() {
        val s = TermScreen(rows = 5, cols = 20)
        s.write("\u001b[31mR\u001b[0mN")
        assertEquals(null, s.rowsSnapshot()[0][1].fg)
    }

    @Test
    fun `bold`() {
        val s = TermScreen(rows = 5, cols = 20)
        s.write("\u001b[1mB")
        assertTrue(s.rowsSnapshot()[0][0].bold)
    }

    @Test
    fun `underline`() {
        val s = TermScreen(rows = 5, cols = 20)
        s.write("\u001b[4mU")
        assertTrue(s.rowsSnapshot()[0][0].underline)
    }

    @Test
    fun `truecolor rgb`() {
        val s = TermScreen(rows = 5, cols = 20)
        s.write("\u001b[38;2;255;0;0mR")
        assertTrue(s.rowsSnapshot()[0][0].fg != null)
    }

    @Test
    fun `256 color`() {
        val s = TermScreen(rows = 5, cols = 20)
        s.write("\u001b[38;5;100mX")
        assertTrue(s.rowsSnapshot()[0][0].fg != null)
    }

    // --- cursor / erase ---

    @Test
    fun `clear screen`() {
        val s = TermScreen(rows = 5, cols = 20)
        s.write("hello")
        s.write("\u001b[2J")
        assertEquals(' ', s.rowsSnapshot()[0][0].ch)
    }

    @Test
    fun `cursor home`() {
        val s = TermScreen(rows = 5, cols = 20)
        s.write("abc")
        s.write("\u001b[H")
        s.write("X")
        val r = s.rowsSnapshot()[0]
        assertEquals('X', r[0].ch)
        assertEquals('b', r[1].ch)
    }

    @Test
    fun `cursor position CUP`() {
        val s = TermScreen(rows = 5, cols = 20)
        s.write("\u001b[3;5HX")
        assertEquals('X', s.rowsSnapshot()[2][4].ch)
    }

    @Test
    fun `erase to end of line`() {
        val s = TermScreen(rows = 5, cols = 20)
        s.write("abcdef\u001b[3D\u001b[K")
        val r = s.rowsSnapshot()[0]
        assertEquals('a', r[0].ch)
        assertEquals('b', r[1].ch)
        assertEquals('c', r[2].ch)
        assertEquals(' ', r[3].ch)
    }

    // --- scroll region ---

    @Test
    fun `scroll region limits scroll`() {
        val s = TermScreen(rows = 5, cols = 10)
        s.write("\u001b[2;4r") // scroll region rows 2-4
        // теперь прокрутим вниз 3 строки
        s.write("\u001b[4;1Hline\n") // на 4-й строке
        s.write("more\n")
        s.write("next")
        val r = s.rowsSnapshot()
        // первая строка не должна измениться (вне scroll region)
        assertEquals(' ', r[0][0].ch)
    }

    // --- find matches ---

    @Test
    fun `find matches basic`() {
        val s = TermScreen(rows = 5, cols = 40)
        s.write("hello world hello")
        assertEquals(2, s.findMatches("hello").size)
    }

    @Test
    fun `find matches case insensitive by default`() {
        val s = TermScreen(rows = 5, cols = 40)
        s.write("Hello HELLO hello")
        assertEquals(3, s.findMatches("hello", caseSensitive = false).size)
    }

    @Test
    fun `find matches case sensitive`() {
        val s = TermScreen(rows = 5, cols = 40)
        s.write("Hello HELLO hello")
        assertEquals(1, s.findMatches("Hello", caseSensitive = true).size)
    }

    @Test
    fun `find empty returns empty`() {
        val s = TermScreen(rows = 5, cols = 40)
        s.write("abc")
        assertTrue(s.findMatches("").isEmpty())
    }

    // --- snapshot ---

    @Test
    fun `snapshot returns text`() {
        val s = TermScreen(rows = 3, cols = 10)
        s.write("hello\nworld")
        val snap = s.snapshot()
        assertTrue(snap.contains("hello"))
        assertTrue(snap.contains("world"))
    }

    // --- unicode ---

    @Test
    fun `cyrillic chars stored`() {
        val s = TermScreen(rows = 3, cols = 20)
        s.write("Привет")
        val r = s.rowsSnapshot()[0]
        assertEquals('П', r[0].ch)
        assertEquals('р', r[1].ch)
        assertEquals('и', r[2].ch)
    }

    @Test
    fun `line wrap on overflow`() {
        val s = TermScreen(rows = 3, cols = 5)
        s.write("abcdefghij")
        val r = s.rowsSnapshot()
        assertEquals('a', r[0][0].ch)
        assertEquals('e', r[0][4].ch)
        assertEquals('f', r[1][0].ch)
    }
}
