package app.termosh.feature.terminal.mosh

import app.termosh.core.common.AppLogger
import app.termosh.core.mosh.MoshManager
import app.termosh.core.mosh.MoshProcess
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Держит активные mosh-процессы по tabId.
 * Позволяет другим частям VM писать в PTY mosh единообразно.
 */
@Singleton
class MoshController @Inject constructor(
    private val moshManager: MoshManager,
) {
    private val procs = mutableMapOf<String, MoshProcess>()

    fun register(tabId: String, process: MoshProcess) {
        procs[tabId] = process
        AppLogger.i("MoshController: зарегистрирован tabId=$tabId, udp=${process.udpPort}")
    }

    fun get(tabId: String): MoshProcess? = procs[tabId]

    fun has(tabId: String): Boolean = procs.containsKey(tabId)

    fun write(tabId: String, bytes: ByteArray): Boolean {
        val p = procs[tabId] ?: return false
        return try {
            p.process.outputStream.apply {
                write(bytes)
                flush()
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun stop(tabId: String) {
        val p = procs.remove(tabId) ?: return
        AppLogger.i("MoshController: остановка tabId=$tabId")
        moshManager.stop(p)
    }

    fun stopAll() {
        procs.keys.toList().forEach { stop(it) }
    }
}
