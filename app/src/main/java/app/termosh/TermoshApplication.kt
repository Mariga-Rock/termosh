package app.termosh

import android.app.Application
import app.termosh.core.common.AppLogger
import dagger.hilt.android.HiltAndroidApp
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import android.os.Environment
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltAndroidApp
class TermoshApplication : Application() {
    override fun onCreate() {
        installCrashLogger()


        super.onCreate()
        AppLogger.init(this)
        app.termosh.core.common.AuditLog.init(this)
        AppLogger.i("TermoshApplication: onCreate")

        // Android содержит урезанный BouncyCastle (без X25519, Ed25519).
        // sshj требует эти алгоритмы — заменяем системный BC на полный.
        try {
            Security.removeProvider("BC")
            Security.addProvider(BouncyCastleProvider())
            AppLogger.i("BC provider: full version installed")
        } catch (t: Throwable) {
            AppLogger.e("BC provider install failed", t)
        }

        val handler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AppLogger.e("UNCAUGHT in ${thread.name}", throwable)
            handler?.uncaughtException(thread, throwable)
        }
    }

    

    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val text = buildString {
                    append("=== ")
                    append(ts)
                    append(" ===\n")
                    append("Thread: ")
                    append(t.name)
                    append("\n")
                    append(sw.toString())
                    append("\n\n")
                }
                getExternalFilesDir(null)?.let { File(it, "crash.log").appendText(text) }
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, "crash.log").appendText(text) }
                File(filesDir, "crash.log").appendText(text)
                runCatching {
                    val pub = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (pub != null && (pub.exists() || pub.mkdirs())) {
                        File(pub, "termosh-crash.log").appendText(text)
                    }
                }
            } catch (_: Throwable) {}
            previous?.uncaughtException(t, e)
        }
    }
}
