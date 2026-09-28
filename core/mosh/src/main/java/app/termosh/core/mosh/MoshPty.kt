package app.termosh.core.mosh

import android.os.ParcelFileDescriptor
import java.io.Closeable
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Локальный PTY вокруг mosh-client (или любого интерактивного бинарника).
 *
 * mosh-client вызывает tcgetattr() на старте и без настоящего PTY падает
 * с "Inappropriate ioctl for device". PTY создаётся в JNI (см. mosh_jni.c).
 */
class MoshPty private constructor(
    private val masterFd: Int,
    private val childPid: Int,
) : Closeable {

    private val pfd: ParcelFileDescriptor = ParcelFileDescriptor.adoptFd(masterFd)
    val input: InputStream = FileInputStream(pfd.fileDescriptor)
    val output: OutputStream = FileOutputStream(pfd.fileDescriptor)

    val pid: Int get() = childPid

    @Volatile
    private var closed = false

    val isClosed: Boolean get() = closed

    val isAlive: Boolean
        get() = !closed && nativeIsAlive(childPid)

    fun resize(rows: Int, cols: Int) {
        if (!closed) nativeResize(masterFd, rows, cols)
    }

    fun waitFor(): Int = nativeWaitFor(childPid)

    override fun close() {
        if (closed) return
        closed = true
        runCatching { pfd.close() }
    }

    companion object {
        init { System.loadLibrary("mosh_pty") }

        fun spawn(
            linker: String,
            binary: String,
            args: List<String>,
            env: Map<String, String>,
            cwd: String?,
            rows: Int,
            cols: Int,
        ): MoshPty {
            val argv = (listOf(linker, binary) + args).toTypedArray()
            val envp = env.map { "${it.key}=${it.value}" }.toTypedArray()
            val r = nativeSpawn(argv, envp, cwd, rows, cols)
            val fd = r[0]; val pid = r[1]
            check(fd >= 0) { "nativeSpawn: masterFd=$fd pid=$pid (см. logcat MoshPty)" }
            check(pid > 0) { "nativeSpawn: bad pid=$pid" }
            return MoshPty(fd, pid)
        }

        @JvmStatic private external fun nativeSpawn(
            argv: Array<String>, envp: Array<String>,
            cwd: String?, rows: Int, cols: Int
        ): IntArray

        @JvmStatic private external fun nativeResize(fd: Int, rows: Int, cols: Int)

        @JvmStatic private external fun nativeWaitFor(pid: Int): Int

        @JvmStatic private external fun nativeIsAlive(pid: Int): Boolean
    }
}
