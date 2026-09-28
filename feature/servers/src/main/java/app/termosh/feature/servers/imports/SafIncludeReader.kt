package app.termosh.feature.servers.imports

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import app.termosh.core.ssh.imports.IncludeReader
import java.io.File

/**
 * IncludeReader поверх SAF DocumentFile или обычного filesystem.
 *
 * Если uri указывает на директорию (DocumentFile.isDirectory) — Include разворачивается.
 * Если схема file:// — работает через java.io.File.
 * Иначе возвращает null, и Include пропускается с warning.
 */
internal class SafIncludeReader private constructor(
    private val context: Context,
    private val rootDir: DocumentFile?,
    private val realRoot: File?,
) : IncludeReader {

    override fun read(path: String): String? {
        realRoot?.let { _ ->
            val f = File(path)
            return if (f.exists() && f.canRead())
                runCatching { f.readText(Charsets.UTF_8) }.getOrNull() else null
        }
        val root = rootDir ?: return null
        val rel = path.substringAfter("ssh/", missingDelimiterValue = path).trimStart('/')
        val doc = resolve(root, rel) ?: return null
        return runCatching {
            context.contentResolver.openInputStream(doc.uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            }
        }.getOrNull()
    }

    override fun list(pattern: String): List<String> {
        realRoot?.let { _ ->
            val f = File(pattern)
            val dir = f.parentFile ?: return emptyList()
            if (!dir.exists()) return emptyList()
            val rx = globToRegex(f.name)
            return dir.listFiles()?.filter { rx.matches(it.name) }?.map { it.absolutePath }.orEmpty()
        }
        val root = rootDir ?: return emptyList()
        val rel = pattern.substringAfter("ssh/", missingDelimiterValue = pattern).trimStart('/')
        val dirPart = rel.substringBeforeLast('/', "")
        val namePattern = rel.substringAfterLast('/', "")
        val dir = if (dirPart.isEmpty()) root else resolve(root, dirPart) ?: return emptyList()
        val rx = globToRegex(namePattern)
        return dir.listFiles()
            .filter { rx.matches(it.name ?: "") }
            .map { it.uri.toString() }
    }

    private fun resolve(dir: DocumentFile, rel: String): DocumentFile? {
        var cur: DocumentFile = dir
        for (seg in rel.split('/').filter { it.isNotEmpty() }) {
            cur = cur.findFile(seg) ?: return null
        }
        return cur
    }

    companion object {
        fun tryCreate(context: Context, uri: Uri): IncludeReader? {
            if (uri.scheme == "file") {
                val f = uri.path?.let { File(it) } ?: return null
                return SafIncludeReader(context, null, f.parentFile)
            }
            val doc = runCatching { DocumentFile.fromSingleUri(context, uri) }.getOrNull()
            val tree = doc?.parentFile
            return if (tree != null && tree.isDirectory) SafIncludeReader(context, tree, null) else null
        }

        fun basePathFor(uri: Uri): String? {
            if (uri.scheme == "file") return uri.path?.substringBeforeLast('/')
            val s = uri.toString()
            val i = s.lastIndexOf("/document/")
            return if (i > 0) s.substring(0, i + 10) else null
        }
    }

    private fun globToRegex(glob: String): Regex {
        val sb = StringBuilder()
        for (c in glob) {
            when (c) {
                '*' -> sb.append(".*")
                '?' -> sb.append('.')
                '.', '(', ')', '[', ']', '{', '}', '+', '^', '$', '\\', '|' ->
                    sb.append('\\').append(c)
                else -> sb.append(c)
            }
        }
        return Regex(sb.toString())
    }
}
