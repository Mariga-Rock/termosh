package app.termosh.feature.terminal

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key

object KeyMapper {
    fun map(event: KeyEvent): String? {
        val ctrl = event.isCtrlPressed
        val alt = event.isAltPressed
        val shift = event.isShiftPressed

        if (ctrl && !alt) {
            val c = letterFor(event.key)
            if (c != null) return ((c.uppercaseChar().code - 'A'.code + 1).toChar()).toString()
            when (event.key) {
                Key.Spacebar -> return "\u0000"
                Key.LeftBracket -> return "\u001b"
                Key.RightBracket -> return "\u001d"
                Key.Backslash -> return "\u001c"
                Key.Apostrophe -> return "\u0007"
            }
        }

        if (alt && !ctrl) {
            val c = letterFor(event.key) ?: plainChar(event.key)
            if (c != null) return "\u001b$c"
        }

        return when (event.key) {
            Key.DirectionUp -> if (ctrl) "\u001b[1;5A" else if (shift) "\u001b[1;2A" else "\u001b[A"
            Key.DirectionDown -> if (ctrl) "\u001b[1;5B" else if (shift) "\u001b[1;2B" else "\u001b[B"
            Key.DirectionRight -> if (ctrl) "\u001b[1;5C" else if (shift) "\u001b[1;2C" else "\u001b[C"
            Key.DirectionLeft -> if (ctrl) "\u001b[1;5D" else if (shift) "\u001b[1;2D" else "\u001b[D"
            Key.MoveHome -> "\u001b[H"
            Key.MoveEnd -> "\u001b[F"
            Key.PageUp -> "\u001b[5~"
            Key.PageDown -> "\u001b[6~"
            Key.Insert -> "\u001b[2~"
            Key.Delete -> "\u001b[3~"
            Key.Tab -> if (shift) "\u001b[Z" else "\t"
            Key.Enter -> "\r"
            Key.Backspace -> "\u007F"
            Key.Escape -> "\u001b"
            Key.Spacebar -> " "
            Key.F1 -> "\u001bOP"
            Key.F2 -> "\u001bOQ"
            Key.F3 -> "\u001bOR"
            Key.F4 -> "\u001bOS"
            Key.F5 -> "\u001b[15~"
            Key.F6 -> "\u001b[17~"
            Key.F7 -> "\u001b[18~"
            Key.F8 -> "\u001b[19~"
            Key.F9 -> "\u001b[20~"
            Key.F10 -> "\u001b[21~"
            Key.F11 -> "\u001b[23~"
            Key.F12 -> "\u001b[24~"
            else -> null
        }
    }

    private fun letterFor(key: Key): Char? = when (key) {
        Key.A -> 'A'; Key.B -> 'B'; Key.C -> 'C'; Key.D -> 'D'; Key.E -> 'E'
        Key.F -> 'F'; Key.G -> 'G'; Key.H -> 'H'; Key.I -> 'I'; Key.J -> 'J'
        Key.K -> 'K'; Key.L -> 'L'; Key.M -> 'M'; Key.N -> 'N'; Key.O -> 'O'
        Key.P -> 'P'; Key.Q -> 'Q'; Key.R -> 'R'; Key.S -> 'S'; Key.T -> 'T'
        Key.U -> 'U'; Key.V -> 'V'; Key.W -> 'W'; Key.X -> 'X'; Key.Y -> 'Y'
        Key.Z -> 'Z'
        else -> null
    }

    private fun plainChar(key: Key): String? = when (key) {
        Key.Zero -> "0"; Key.One -> "1"; Key.Two -> "2"; Key.Three -> "3"
        Key.Four -> "4"; Key.Five -> "5"; Key.Six -> "6"; Key.Seven -> "7"
        Key.Eight -> "8"; Key.Nine -> "9"
        Key.Minus -> "-"; Key.Equals -> "="
        Key.LeftBracket -> "["; Key.RightBracket -> "]"
        Key.Semicolon -> ";"; Key.Apostrophe -> "'"
        Key.Comma -> ","; Key.Period -> "."; Key.Slash -> "/"
        Key.Backslash -> "\\"; Key.Grave -> "`"
        else -> null
    }
}
