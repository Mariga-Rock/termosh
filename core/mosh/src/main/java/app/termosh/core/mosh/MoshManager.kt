package app.termosh.core.mosh

import android.content.Context
import app.termosh.core.ssh.SshSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

class MoshProcess(
    val pty: MoshPty,
    val udpPort: String,
    val host: String,
) {
    /** Фасад для кода, который привык к java.lang.Process. */
    val process: Facade get() = Facade()
    inner class Facade {
        val inputStream: java.io.InputStream get() = pty.input
        val outputStream: java.io.OutputStream get() = pty.output
        val isAlive: Boolean get() = pty.isAlive
        fun destroy() { pty.close() }
        fun waitFor(): Int = pty.waitFor()
        fun exitValue(): Int = pty.waitFor()
    }
    fun resize(rows: Int, cols: Int) = pty.resize(rows, cols)
    fun close() = pty.close()
}

@Singleton
class MoshManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun start(
        host: String,
        sshSession: SshSession,
        columns: Int = 80,
        lines: Int = 24,
        sshPort: Int = 22,
        useTmux: Boolean = false,
    ): MoshProcess = withContext(Dispatchers.IO) {

        // 1. Распаковать terminfo из assets в filesDir (один раз)
        val terminfoPath = copyTerminfoFromAssets()

        // 1b. Проверяем tmux, если запрошены постоянные сессии
        val hasTmux: Boolean = if (useTmux) {
            val out = runCatching {
                sshSession.exec("command -v tmux 2>/dev/null", timeoutSec = 5)
            }.getOrDefault("")
            out.trim().isNotEmpty()
        } else false

        // 2. Запустить mosh-server на удалённой стороне
        val cmd = "mosh-server new -s -c 256 -l LANG=en_US.UTF-8"
        val output = sshSession.exec(cmd, timeoutSec = 20)

        val connectLine = output.lineSequence()
            .firstOrNull { it.startsWith("MOSH CONNECT ") }
            ?: error("mosh-server не запустился или не установлен на сервере. Ответ: $output")

        val parts = connectLine.trim().split(" ")
        if (parts.size < 4) error("Некорректный ответ mosh-server: $connectLine")

        val udpPort = parts[2]
        val key = parts[3]

        // 3. Копируем libmosh-client в filesDir — на случай, если nativeLibraryDir noexec
        val srcLib = File(context.applicationInfo.nativeLibraryDir, "libmosh-client.so")
        if (!srcLib.exists()) error("libmosh-client.so не найден: ${srcLib.absolutePath}")
        val clientFile = File(context.filesDir, "mosh-client")
        if (!clientFile.exists() || clientFile.length() != srcLib.length()) {
            srcLib.copyTo(clientFile, overwrite = true)
        }
        clientFile.setExecutable(true, false)

        // 4. Запуск mosh-client под локальным PTY через linker64
        val linker = if (android.os.Build.SUPPORTED_64_BIT_ABIS.isNotEmpty())
            "/system/bin/linker64" else "/system/bin/linker"
        val env = mapOf(
            "MOSH_KEY" to key,
            "TERM" to "xterm-256color",
            "TERMINFO" to terminfoPath,
            "LANG" to "en_US.UTF-8",
            "LC_ALL" to "en_US.UTF-8",
            "COLUMNS" to columns.toString(),
            "LINES" to lines.toString(),
            "HOME" to context.filesDir.absolutePath,
            "PATH" to "${context.applicationInfo.nativeLibraryDir}:/system/bin",
            "LD_LIBRARY_PATH" to context.applicationInfo.nativeLibraryDir,
        )
        val pty = MoshPty.spawn(
            linker = linker,
            binary = clientFile.absolutePath,
            args = listOf(host, udpPort),
            env = env,
            cwd = context.filesDir.absolutePath,
            rows = lines,
            cols = columns,
        )

        // 5. Проверка, что клиент не упал в первые 700 мс
        // 4b. Если tmux включён и доступен — отправим attach-команду в PTY
        if (useTmux && hasTmux) {
            kotlinx.coroutines.delay(500)
            runCatching {
                val cmd = "tmux attach -t termosh 2>/dev/null || tmux new -s termosh\n"
                pty.output.write(cmd.toByteArray(Charsets.UTF_8))
                pty.output.flush()
            }
        }

        kotlinx.coroutines.delay(700)
        if (!pty.isAlive) {
            val err = runCatching {
                val buf = ByteArray(2048)
                val n = pty.input.read(buf)
                if (n > 0) String(buf, 0, n) else ""
            }.getOrElse { "" }
            runCatching { pty.close() }
            error("libmosh-client упал. stderr: $err")
        }

        return@withContext MoshProcess(pty, udpPort, host)
    }

    fun stop(mosh: MoshProcess) {
        runCatching { mosh.process.destroy() }
        runCatching { mosh.process.waitFor() }
    }

    /**
     * Копирует assets/terminfo -> filesDir/terminfo (рекурсивно).
     * Возвращает абсолютный путь к filesDir/terminfo.
     */
    private fun copyTerminfoFromAssets(): String {
        val target = File(context.filesDir, "terminfo")
        val marker = File(target, ".copied")
        if (marker.exists()) {
            return target.absolutePath
        }

        target.mkdirs()

        fun copyDir(assetPath: String, targetDir: File) {
            val entries = context.assets.list(assetPath) ?: return
            for (name in entries) {
                val fullAsset = if (assetPath.isEmpty()) name else "$assetPath/$name"
                val subEntries = context.assets.list(fullAsset)
                if (subEntries != null && subEntries.isNotEmpty()) {
                    // Это директория
                    val subDir = File(targetDir, name)
                    subDir.mkdirs()
                    copyDir(fullAsset, subDir)
                } else {
                    // Это файл
                    runCatching {
                        val out = File(targetDir, name)
                        context.assets.open(fullAsset).use { inp ->
                            out.outputStream().use { o -> inp.copyTo(o) }
                        }
                    }
                }
            }
        }

        copyDir("terminfo", target)
        marker.writeText("ok")
        return target.absolutePath
    }
}
