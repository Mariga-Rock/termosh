package app.termosh.core.terminal

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TermCell(
    val ch: Char = ' ',
    val fg: Color? = null,
    val bg: Color? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
)

/**
 * Терминальный экран как сетка rows × cols.
 * Поддерживает:
 *  - cursor position (CUP, CUU/CUD/CUF/CUB, CHA, VPA)
 *  - erase (ED, EL)
 *  - insert/delete lines/chars
 *  - scroll region (DECSTBM)
 *  - alternate screen buffer (1049)
 *  - SGR (цвета, bold, italic, underline)
 */
class TermScreen(val rows: Int = 24, val cols: Int = 80) {

    private val main = Array(rows) { Array(cols) { TermCell() } }
    private val alt = Array(rows) { Array(cols) { TermCell() } }
    private var grid = main
    private var usingAlt = false

    var cursorRow = 0
    var cursorCol = 0
    var cursorVisible = true
    var altScreenActive = false

    private var scrollTop = 0
    private var scrollBottom = rows - 1

    private var curFg: Color? = null
    private var curBg: Color? = null
    private var curBold = false
    private var curItalic = false
    private var curUnderline = false

    private val _snapshot = MutableStateFlow(0L)
    val snapshot: StateFlow<Long> = _snapshot.asStateFlow()

    fun rowsSnapshot(): List<List<TermCell>> = grid.map { it.toList() }

    private var escBuf = StringBuilder()
    private var state = 0

    @Synchronized
    fun write(text: String) {
        for (ch in text) process(ch)
        _snapshot.value = _snapshot.value + 1
    }

    @Synchronized
    fun clear() {
        for (r in 0 until rows) for (c in 0 until cols) grid[r][c] = TermCell()
        cursorRow = 0; cursorCol = 0
        _snapshot.value = _snapshot.value + 1
    }


    fun snapshot(): String {
        val sb = StringBuilder()
        for (r in 0 until rows) {
            val line = StringBuilder()
            for (c in 0 until cols) line.append(grid[r][c].ch)
            sb.append(line.toString().trimEnd()).append('\n')
        }
        return sb.toString()
    }


    /**
     * Возвращает список (row, col) для подсветки всех вхождений query.
     */
    fun findMatches(query: String, caseSensitive: Boolean = false): List<Pair<Int, Int>> {
        if (query.isEmpty()) return emptyList()
        val result = mutableListOf<Pair<Int, Int>>()
        val q = if (caseSensitive) query else query.lowercase()
        for (r in 0 until rows) {
            val line = StringBuilder()
            for (c in 0 until cols) line.append(grid[r][c].ch)
            val s = if (caseSensitive) line.toString() else line.toString().lowercase()
            var idx = s.indexOf(q)
            while (idx >= 0) {
                result += r to idx
                idx = s.indexOf(q, idx + 1)
            }
        }
        return result
    }

    private fun process(ch: Char) {
        when (state) {
            0 -> normal(ch)
            1 -> esc(ch)
            2 -> csi(ch)
            3 -> osc(ch)
        }
    }

    private fun normal(ch: Char) {
        when (ch) {
            '\u001b' -> { state = 1; escBuf.clear() }
            '\n' -> lineFeed()
            '\r' -> cursorCol = 0
            '\b' -> if (cursorCol > 0) cursorCol--
            '\t' -> cursorCol = minOf(cols - 1, (cursorCol / 8 + 1) * 8)
            '\u0007' -> {}
            else -> if (ch.code >= 32) putChar(ch)
        }
    }

    private fun esc(ch: Char) {
        when (ch) {
            '[' -> { escBuf.clear(); state = 2 }
            ']' -> { escBuf.clear(); state = 3 }
            'D' -> lineFeed()
            'M' -> reverseLineFeed()
            'c' -> clear()
            else -> state = 0
        }
    }

    private fun osc(ch: Char) {
        when (ch) {
            '\u0007' -> state = 0
            '\u001b' -> state = 0
            else -> {}
        }
    }

    private fun csi(ch: Char) {
        if (ch in '0'..'9' || ch == ';' || ch == '?') { escBuf.append(ch); return }
        val params = escBuf.toString()
        when (ch) {
            'm' -> sgr(params)
            'H', 'f' -> cup(params)
            'A' -> cursorUp(intParam(params, 1))
            'B' -> cursorDown(intParam(params, 1))
            'C' -> cursorRight(intParam(params, 1))
            'D' -> cursorLeft(intParam(params, 1))
            'G' -> cursorCol = (intParam(params, 1) - 1).coerceIn(0, cols - 1)
            'd' -> cursorRow = (intParam(params, 1) - 1).coerceIn(0, rows - 1)
            'J' -> eraseDisplay(intParam(params, 0))
            'K' -> eraseLine(intParam(params, 0))
            'L' -> insertLines(intParam(params, 1))
            'M' -> deleteLines(intParam(params, 1))
            'P' -> deleteChars(intParam(params, 1))
            '@' -> insertChars(intParam(params, 1))
            'S' -> scrollUpRegion(intParam(params, 1))
            'T' -> scrollDownRegion(intParam(params, 1))
            'h' -> setMode(params, true)
            'l' -> setMode(params, false)
            'r' -> setScrollRegion(params)
            else -> {}
        }
        state = 0
    }

    private fun intParam(s: String, def: Int): Int {
        if (s.isEmpty()) return def
        val first = s.split(';').firstOrNull { it.isNotEmpty() && it != "?" } ?: return def
        return first.toIntOrNull() ?: def
    }

    private fun cup(params: String) {
        val parts = params.split(';')
        val r = (parts.getOrNull(0)?.toIntOrNull() ?: 1) - 1
        val c = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
        cursorRow = r.coerceIn(0, rows - 1)
        cursorCol = c.coerceIn(0, cols - 1)
    }

    private fun cursorUp(n: Int) { cursorRow = (cursorRow - n).coerceAtLeast(0) }
    private fun cursorDown(n: Int) { cursorRow = (cursorRow + n).coerceAtMost(rows - 1) }
    private fun cursorRight(n: Int) { cursorCol = (cursorCol + n).coerceAtMost(cols - 1) }
    private fun cursorLeft(n: Int) { cursorCol = (cursorCol - n).coerceAtLeast(0) }

    private fun putChar(ch: Char) {
        if (cursorCol >= cols) {
            cursorCol = 0
            lineFeed()
        }
        grid[cursorRow][cursorCol] = TermCell(
            ch = ch,
            fg = curFg,
            bg = curBg,
            bold = curBold,
            italic = curItalic,
            underline = curUnderline,
        )
        cursorCol++
    }

    private fun lineFeed() {
        if (cursorRow == scrollBottom) {
            scrollUpRegion(1)
        } else {
            cursorRow = (cursorRow + 1).coerceAtMost(rows - 1)
        }
    }

    private fun reverseLineFeed() {
        if (cursorRow == scrollTop) {
            scrollDownRegion(1)
        } else {
            cursorRow = (cursorRow - 1).coerceAtLeast(0)
        }
    }

    private fun scrollUpRegion(n: Int) {
        for (i in 0 until n) {
            for (r in scrollTop until scrollBottom) {
                grid[r] = grid[r + 1].copyOf()
            }
            for (c in 0 until cols) grid[scrollBottom][c] = TermCell()
        }
    }

    private fun scrollDownRegion(n: Int) {
        for (i in 0 until n) {
            for (r in scrollBottom downTo scrollTop + 1) {
                grid[r] = grid[r - 1].copyOf()
            }
            for (c in 0 until cols) grid[scrollTop][c] = TermCell()
        }
    }

    private fun eraseDisplay(mode: Int) {
        when (mode) {
            0 -> {
                for (c in cursorCol until cols) grid[cursorRow][c] = TermCell()
                for (r in cursorRow + 1 until rows) for (c in 0 until cols) grid[r][c] = TermCell()
            }
            1 -> {
                for (c in 0..cursorCol) grid[cursorRow][c] = TermCell()
                for (r in 0 until cursorRow) for (c in 0 until cols) grid[r][c] = TermCell()
            }
            2, 3 -> clear()
        }
    }

    private fun eraseLine(mode: Int) {
        when (mode) {
            0 -> for (c in cursorCol until cols) grid[cursorRow][c] = TermCell()
            1 -> for (c in 0..cursorCol) grid[cursorRow][c] = TermCell()
            2 -> for (c in 0 until cols) grid[cursorRow][c] = TermCell()
        }
    }

    private fun insertLines(n: Int) {
        if (cursorRow !in scrollTop..scrollBottom) return
        for (i in 0 until n) {
            for (r in scrollBottom downTo cursorRow + 1) grid[r] = grid[r - 1].copyOf()
            for (c in 0 until cols) grid[cursorRow][c] = TermCell()
        }
    }

    private fun deleteLines(n: Int) {
        if (cursorRow !in scrollTop..scrollBottom) return
        for (i in 0 until n) {
            for (r in cursorRow until scrollBottom) grid[r] = grid[r + 1].copyOf()
            for (c in 0 until cols) grid[scrollBottom][c] = TermCell()
        }
    }

    private fun deleteChars(n: Int) {
        for (i in 0 until n) {
            for (c in cursorCol until cols - 1) grid[cursorRow][c] = grid[cursorRow][c + 1]
            grid[cursorRow][cols - 1] = TermCell()
        }
    }

    private fun insertChars(n: Int) {
        for (i in 0 until n) {
            for (c in cols - 1 downTo cursorCol + 1) grid[cursorRow][c] = grid[cursorRow][c - 1]
            grid[cursorRow][cursorCol] = TermCell()
        }
    }

    private fun setScrollRegion(params: String) {
        val parts = params.split(';')
        scrollTop = ((parts.getOrNull(0)?.toIntOrNull() ?: 1) - 1).coerceIn(0, rows - 1)
        scrollBottom = ((parts.getOrNull(1)?.toIntOrNull() ?: rows) - 1).coerceIn(0, rows - 1)
        if (scrollTop > scrollBottom) {
            val t = scrollTop; scrollTop = scrollBottom; scrollBottom = t
        }
    }

    private fun setMode(params: String, set: Boolean) {
        val clean = params.trimStart('?')
        when (clean) {
            "25" -> cursorVisible = set
            "1049" -> {
                if (set) {
                    usingAlt = true
                    altScreenActive = true
                    grid = alt
                    for (r in 0 until rows) for (c in 0 until cols) grid[r][c] = TermCell()
                    cursorRow = 0; cursorCol = 0
                } else {
                    usingAlt = false
                    altScreenActive = false
                    grid = main
                }
            }
        }
    }

    private fun sgr(params: String) {
        val parts = if (params.isEmpty()) listOf(0) else params.split(';').mapNotNull { it.toIntOrNull() }
        if (parts.isEmpty()) { resetSgr(); return }
        var i = 0
        while (i < parts.size) {
            when (val p = parts[i]) {
                0 -> resetSgr()
                1 -> curBold = true
                3 -> curItalic = true
                4 -> curUnderline = true
                22 -> curBold = false
                23 -> curItalic = false
                24 -> curUnderline = false
                in 30..37 -> curFg = Color(ANSI_16[p - 30])
                39 -> curFg = null
                in 40..47 -> curBg = Color(ANSI_16[p - 40])
                49 -> curBg = null
                in 90..97 -> curFg = Color(ANSI_16[p - 90 + 8])
                in 100..107 -> curBg = Color(ANSI_16[p - 100 + 8])
                38 -> {
                    if (i + 1 < parts.size && parts[i + 1] == 5 && i + 2 < parts.size) {
                        curFg = color256(parts[i + 2]); i += 2
                    } else if (i + 1 < parts.size && parts[i + 1] == 2 && i + 4 < parts.size) {
                        curFg = Color(0xFF000000.toInt() or (parts[i + 2] shl 16) or (parts[i + 3] shl 8) or parts[i + 4])
                        i += 4
                    }
                }
                48 -> {
                    if (i + 1 < parts.size && parts[i + 1] == 5 && i + 2 < parts.size) {
                        curBg = color256(parts[i + 2]); i += 2
                    } else if (i + 1 < parts.size && parts[i + 1] == 2 && i + 4 < parts.size) {
                        curBg = Color(0xFF000000.toInt() or (parts[i + 2] shl 16) or (parts[i + 3] shl 8) or parts[i + 4])
                        i += 4
                    }
                }
            }
            i++
        }
    }

    private fun resetSgr() {
        curFg = null; curBg = null
        curBold = false; curItalic = false; curUnderline = false
    }

    companion object {
        private val ANSI_16 = intArrayOf(
            0xFF1A1B26.toInt(), 0xFFF7768E.toInt(), 0xFF9ECE6A.toInt(), 0xFFE0AF68.toInt(),
            0xFF7AA2F7.toInt(), 0xFFBB9AF7.toInt(), 0xFF7DCFFF.toInt(), 0xFFC0CAF5.toInt(),
            0xFF565F89.toInt(), 0xFFFF7A93.toInt(), 0xFFB9F27C.toInt(), 0xFFFF9E64.toInt(),
            0xFF7DA6FF.toInt(), 0xFFBB9AF7.toInt(), 0xFF0DB9D7.toInt(), 0xFFFFFFFF.toInt(),
        )
        private fun color256(n: Int): Color = when {
            n < 16 -> Color(ANSI_16[n])
            n in 16..231 -> {
                val idx = n - 16
                val r = idx / 36
                val g = (idx % 36) / 6
                val b = idx % 6
                Color(
                    0xFF000000.toInt() or
                        (if (r == 0) 0 else 55 + r * 40) shl 16 or
                        (if (g == 0) 0 else 55 + g * 40) shl 8 or
                        (if (b == 0) 0 else 55 + b * 40)
                )
            }
            else -> {
                val v = (n - 232) * 10 + 8
                Color(0xFF000000.toInt() or (v shl 16) or (v shl 8) or v)
            }
        }
    }
}
