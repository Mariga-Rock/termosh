package app.termosh.core.common

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Логгер с двумя режимами доступа:
 *  - через Hilt (@Inject AppLogger) — предпочтительно;
 *  - через companion-объект (AppLogger.i(...)) — для обратной совместимости
 *    в местах, где DI недоступен (например, в статическом коде).
 *
 * Единственный экземпляр создаётся Hilt при первом обращении.
 * Application.init() создаёт его раньше DI-графа, чтобы лог работал с самого старта.
 */
@Singleton
class AppLogger @Inject constructor(
    @ApplicationContext context: Context,
) {
    companion object {
        private const val TAG = "Termosh"
        private const val LOG_FILE = "termosh.log"
        private const val MAX_SIZE_BYTES = 2L * 1024 * 1024

        @Volatile private var instance: AppLogger? = null
        private val lock = Any()

        /** Вызывается из Application.onCreate ДО старта Hilt. */
        fun init(ctx: Context) {
            synchronized(lock) {
                if (instance == null) instance = AppLogger(ctx)
            }
        }

        /** Вызывается Hilt-модулем, чтобы не создать второй экземпляр. */
        fun setInstance(logger: AppLogger) {
            synchronized(lock) {
                if (instance == null) instance = logger
            }
        }

        fun i(msg: String) { instance?.i(msg) }
        fun w(msg: String) { instance?.w(msg) }
        fun e(msg: String, t: Throwable? = null) { instance?.e(msg, t) }
        fun logFile(): File? = instance?.logFile()
        fun logDir(): File? = instance?.logDir()
        fun clear() { instance?.clear() }
    }

    private val fmt = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
    private val dirObj: File = File(context.filesDir, "logs").apply { mkdirs() }
    private val fileObj: File = File(dirObj, LOG_FILE)

    fun logFile(): File = fileObj
    fun logDir(): File = dirObj

    fun i(msg: String) {
        Log.i(TAG, msg)
        append("I", msg, null)
    }

    fun w(msg: String) {
        Log.w(TAG, msg)
        append("W", msg, null)
    }

    fun e(msg: String, t: Throwable? = null) {
        Log.e(TAG, msg, t)
        append("E", msg, t)
    }

    fun clear() {
        runCatching { fileObj.delete() }
    }

    private fun append(level: String, msg: String, t: Throwable?) {
        runCatching {
            if (fileObj.exists() && fileObj.length() > MAX_SIZE_BYTES) {
                val backup = File(dirObj, "$LOG_FILE.old")
                if (backup.exists()) backup.delete()
                fileObj.renameTo(backup)
            }
            val ts = fmt.format(Date())
            val sb = StringBuilder()
            sb.append(ts).append(" [").append(level).append("] ").append(msg).append('\n')
            if (t != null) {
                sb.append(t.javaClass.name).append(": ")
                    .append(t.message ?: "(no message)").append('\n')
                t.stackTrace.take(30).forEach { el ->
                    sb.append("    at ").append(el.toString()).append('\n')
                }
            }
            fileObj.appendText(sb.toString())
        }
    }
}
