package app.termosh.core.ssh.imports

/**
 * Парсер ~/.ssh/config (формат OpenSSH).
 * Возвращает список Host-блоков в виде карт ключ → значение.
 */
object OpenSshConfigParser {

    data class ParsedHost(
        val hostPatterns: List<String>,
        val hostName: String?,
        val user: String?,
        val port: Int?,
        val identityFiles: List<String>,
        val proxyJump: String?,
    )

    fun parse(text: String): List<ParsedHost> {
        val result = mutableListOf<ParsedHost>()
        var current: MutableMap<String, MutableList<String>>? = null

        fun flush() {
            val map = current ?: return
            val hostPatterns = map["host"]?.flatMap { it.split(" ", "\t") }?.filter { it.isNotBlank() } ?: emptyList()
            if (hostPatterns.isEmpty()) return
            result += ParsedHost(
                hostPatterns = hostPatterns,
                hostName = map["hostname"]?.firstOrNull(),
                user = map["user"]?.firstOrNull(),
                port = map["port"]?.firstOrNull()?.toIntOrNull(),
                identityFiles = map["identityfile"]?.flatMap { it.split("\n") }?.filter { it.isNotBlank() } ?: emptyList(),
                proxyJump = map["proxyjump"]?.firstOrNull(),
            )
        }

        text.lineSequence().forEach { raw ->
            val line = raw.substringBefore('#').trim()
            if (line.isEmpty()) return@forEach

            // Отступ → это свойство текущего Host
            val isIndented = raw.startsWith(" ") || raw.startsWith("\t")
            if (!isIndented) {
                val parts = line.split(Regex("\\s+"), limit = 2)
                val key = parts.getOrNull(0)?.lowercase() ?: return@forEach
                if (key == "host") {
                    flush()
                    current = mutableMapOf()
                    current!!.getOrPut("host") { mutableListOf() }.add(parts.getOrNull(1) ?: "")
                    return@forEach
                }
                // Прочие top-level (Include, Match) — игнорируем
                current = null
                return@forEach
            }

            val map = current ?: return@forEach
            val parts = line.split(Regex("\\s+"), limit = 2)
            val key = parts.getOrNull(0)?.lowercase() ?: return@forEach
            val value = parts.getOrNull(1)?.trim() ?: ""
            map.getOrPut(key) { mutableListOf() }.add(value)
        }
        flush()
        return result
    }
}
