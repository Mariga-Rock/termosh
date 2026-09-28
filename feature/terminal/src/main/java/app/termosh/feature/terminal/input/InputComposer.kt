package app.termosh.feature.terminal.input

import app.termosh.core.common.ShellEscape

/**
 * Собирает payload для PTY: обрабатывает Ctrl/Alt модификаторы
 * и добавляет перевод строки. Без состояния, чистая функция.
 */
object InputComposer {

    fun compose(text: String, modifiers: Set<String>): ByteArray {
        var body: ByteArray = text.toByteArray(Charsets.UTF_8)
        if ("CTRL" in modifiers && text.length == 1) {
            val c = text[0].uppercaseChar()
            body = when {
                c in 'A'..'Z' -> byteArrayOf((c.code - 'A'.code + 1).toByte())
                c == '[' -> byteArrayOf(0x1B)
                c == '\\' -> byteArrayOf(0x1C)
                c == ']' -> byteArrayOf(0x1D)
                else -> body
            }
        }
        val alt = if ("ALT" in modifiers) byteArrayOf(0x1B) else ByteArray(0)
        return alt + body + "\n".toByteArray(Charsets.UTF_8)
    }

    /** Обёртка в одиночные кавычки для export VAR=value. */
    fun escapeShellValue(value: String): String = ShellEscape.quote(value)
}
