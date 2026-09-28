package app.termosh.core.common

/**
 * Экранирование значений для shell.
 * Используется при отправке export VAR=value в PTY.
 */
object ShellEscape {

    /**
     * Оборачивает в одинарные кавычки, экранирует одинарные кавычки внутри.
     * 'foo'         -> 'foo'
     * "it's"        -> 'it'\''s'
     * "a b\nc"     -> 'a b\nc'
     */
    fun quote(value: String): String {
        val sb = StringBuilder(value.length + 2)
        sb.append('\'')
        for (ch in value) {
            if (ch == '\'') sb.append("'\\''")
            else sb.append(ch)
        }
        sb.append('\'')
        return sb.toString()
    }
}
