package app.termosh.core.terminal

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionLogger @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val fmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    private fun dir(): File = File(context.filesDir, "sessions").apply { mkdirs() }

    fun createLogFile(serverName: String): File {
        val safe = serverName.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        return File(dir(), "${safe}_${fmt.format(Date())}.log")
    }

    fun append(file: File, text: String) {
        runCatching { file.appendText(text) }
    }

    fun list(): List<File> =
        dir().listFiles()?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()

    fun read(file: File): String =
        runCatching { file.readText() }.getOrDefault("")

    fun delete(file: File) {
        runCatching { file.delete() }
    }

    fun dirPath(): String = dir().absolutePath
}
