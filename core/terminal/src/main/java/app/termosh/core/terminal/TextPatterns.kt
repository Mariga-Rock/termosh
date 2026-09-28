package app.termosh.core.terminal

/**
 * Regex для поиска кликабельных сущностей в выводе терминала.
 */
object TextPatterns {

    val URL = Regex("""(https?://[^\s]+|ftp://[^\s]+|www\.[^\s]+)""")

    val IP = Regex("""\b(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})(:\d+)?\b""")

    val PATH = Regex("""(/[\w./-]{3,})""")

    /** Нормализует URL: добавляет https:// если нужно. */
    fun normalizeUrl(matched: String): String =
        if (matched.startsWith("http")) matched else "https://$matched"
}
