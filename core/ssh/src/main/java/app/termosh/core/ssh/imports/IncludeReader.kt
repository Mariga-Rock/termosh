package app.termosh.core.ssh.imports

/**
 * Абстракция над файловой системой, через которую разворачиваются Include.
 * Реализация в UI может опираться на File, DocumentFile (SAF) и т. п.
 */
interface IncludeReader {

    /** Прочитать содержимое файла по пути (после раскрытия ~ и относительных). */
    fun read(path: String): String?

    /**
     * Найти файлы по glob-паттерну (с уже раскрытыми ~ и relative).
     * Поддерживаются '*' и '?'. Возвращает список полных путей.
     */
    fun list(pattern: String): List<String>
}
