package app.termosh.core.ssh.imports

/**
 * Разворачивает директиву Include в тексте ~/.ssh/config.
 *
 * Поддерживает:
 *  - `Include ~/.ssh/config.d/<glob>` — glob с '*' и '?'
 *  - `Include relative/path` — относительно basePath
 *  - `Include /abs/path` — как есть
 *  - рекурсию с защитой от циклов (visited set) и лимитом глубины
 *
 * Директива `Match` не поддерживается — она вырезается из результата,
 * чтобы парсер её не пытался интерпретировать.
 */
object IncludeExpander {

    private const val MAX_DEPTH = 8

    data class Result(
        val text: String,
        val warnings: List<String>,
    )

    fun expand(
        text: String,
        basePath: String,
        reader: IncludeReader,
    ): Result {
        val warnings = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val out = StringBuilder()
        expandInto(
            text = text,
            basePath = basePath,
            reader = reader,
            visited = visited,
            depth = 0,
            warnings = warnings,
            out = out,
        )
        return Result(out.toString(), warnings)
    }

    private fun expandInto(
        text: String,
        basePath: String,
        reader: IncludeReader,
        visited: MutableSet<String>,
        depth: Int,
        warnings: MutableList<String>,
        out: StringBuilder,
    ) {
        if (depth > MAX_DEPTH) {
            warnings += "Include: достигнута максимальная глубина $MAX_DEPTH, дальнейшие вложенные файлы пропущены"
            return
        }

        text.lineSequence().forEach { raw ->
            val trimmed = raw.trim()
            val lower = trimmed.lowercase()

            when {
                lower.startsWith("include") && (trimmed.length == 7 || trimmed[7].isWhitespace()) -> {
                    val patternRaw = trimmed.substring(7).trim()
                    if (patternRaw.isEmpty()) return@forEach
                    val pattern = resolvePath(patternRaw, basePath)
                    val matches = runCatching { reader.list(pattern) }.getOrElse {
                        warnings += "Include $patternRaw: не удалось развернуть glob"
                        emptyList()
                    }
                    if (matches.isEmpty()) {
                        warnings += "Include $patternRaw: файлы не найдены"
                        return@forEach
                    }
                    matches.sorted().forEach { path ->
                        if (path in visited) {
                            warnings += "Include $patternRaw: цикл, $path уже прочитан"
                            return@forEach
                        }
                        visited += path
                        val content = runCatching { reader.read(path) }.getOrNull()
                        if (content == null) {
                            warnings += "Include $patternRaw: не удалось прочитать $path"
                            return@forEach
                        }
                        expandInto(
                            text = content,
                            basePath = path.substringBeforeLast('/', basePath),
                            reader = reader,
                            visited = visited,
                            depth = depth + 1,
                            warnings = warnings,
                            out = out,
                        )
                    }
                }
                lower.startsWith("match") && (trimmed.length == 5 || trimmed[5].isWhitespace()) -> {
                    warnings += "Match не поддерживается — директива пропущена"
                }
                else -> {
                    out.append(raw).append('\n')
                }
            }
        }
    }

    /**
     * Раскрывает "~" и делает путь абсолютным относительно basePath.
     * На Android "~" = basePath (пользователь указал файл внутри ~/.ssh).
     */
    private fun resolvePath(pattern: String, basePath: String): String {
        val noTilde = when {
            pattern == "~" -> basePath
            pattern.startsWith("~/") -> basePath + pattern.removePrefix("~")
            else -> pattern
        }
        return if (noTilde.startsWith("/")) noTilde
        else "$basePath/$noTilde".replace("//", "/")
    }
}
