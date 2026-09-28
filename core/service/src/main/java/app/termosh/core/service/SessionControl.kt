package app.termosh.core.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Связь сервиса (уведомление) с ViewModel.
 * Сервис публикует события, VM подписана.
 */
object SessionControl {
    private val _disconnectAll = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val disconnectAll: SharedFlow<Unit> = _disconnectAll.asSharedFlow()

    private val _status = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val status: SharedFlow<String> = _status.asSharedFlow()

    fun requestDisconnectAll() { _disconnectAll.tryEmit(Unit) }
    fun publishStatus(text: String) { _status.tryEmit(text) }
}
