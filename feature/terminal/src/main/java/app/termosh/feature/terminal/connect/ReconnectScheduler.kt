package app.termosh.feature.terminal.connect

import app.termosh.core.common.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Управляет retry-циклами по tabId.
 * Знает только про backoff и отмену. Что именно переподключать — задача вызывающего.
 */
@Singleton
class ReconnectScheduler @Inject constructor() {

    private val jobs = mutableMapOf<String, Job>()

    fun schedule(
        scope: CoroutineScope,
        tabId: String,
        maxAttempts: Int,
        onAttempt: suspend (attempt: Int, waitMs: Long) -> Unit,
        onReconnect: suspend () -> Boolean,
        onFailed: suspend () -> Unit,
    ) {
        jobs[tabId]?.cancel()
        jobs[tabId] = scope.launch {
            var attempt = 0
            while (attempt < maxAttempts) {
                attempt++
                val waitMs = backoffMs(attempt)
                onAttempt(attempt, waitMs)
                delay(waitMs)
                try {
                    if (onReconnect()) {
                        AppLogger.i("ReconnectScheduler: успех tabId=$tabId за $attempt попыток")
                        return@launch
                    }
                } catch (t: Throwable) {
                    AppLogger.w("ReconnectScheduler: попытка $attempt для tabId=$tabId упала: ${t.message}")
                }
            }
            onFailed()
        }
    }

    fun cancel(tabId: String) {
        jobs.remove(tabId)?.cancel()
    }

    fun cancelAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }

    private fun backoffMs(attempt: Int): Long = when (attempt) {
        1 -> 1_000L
        2 -> 2_000L
        3 -> 4_000L
        4 -> 8_000L
        else -> 15_000L
    }
}
