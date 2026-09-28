package app.termosh.feature.servers.forwards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.termosh.domain.model.PortForward
import app.termosh.domain.usecase.portforward.DeletePortForwardUseCase
import app.termosh.domain.usecase.portforward.ObservePortForwardsUseCase
import app.termosh.domain.usecase.portforward.SavePortForwardUseCase
import app.termosh.domain.usecase.server.GetServerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForwardsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    observeForwards: ObservePortForwardsUseCase,
    private val saveForward: SavePortForwardUseCase,
    private val deleteForward: DeletePortForwardUseCase,
    private val getServer: GetServerUseCase,
) : ViewModel() {

    private val serverId: String = savedState.get<String>("serverId") ?: ""

    private val _state = MutableStateFlow(ForwardsUiState(serverId = serverId))
    val state: StateFlow<ForwardsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                val s = getServer(serverId)
                _state.value = _state.value.copy(serverName = s.name)
            }
        }
        viewModelScope.launch {
            observeForwards(serverId).collect { list ->
                _state.value = _state.value.copy(forwards = list, loading = false)
            }
        }
    }

    fun openNew() {
        _state.value = _state.value.copy(showEditDialog = true, editId = "", editLocal = "8080", editRemoteHost = "127.0.0.1", editRemotePort = "80")
    }

    fun openEdit(f: PortForward) {
        _state.value = _state.value.copy(
            showEditDialog = true, editId = f.id,
            editLocal = f.localPort.toString(),
            editRemoteHost = f.remoteHost,
            editRemotePort = f.remotePort.toString(),
        )
    }

    fun dismiss() { _state.value = _state.value.copy(showEditDialog = false) }
    fun setLocal(v: String) { _state.value = _state.value.copy(editLocal = v.filter { it.isDigit() }) }
    fun setRemoteHost(v: String) { _state.value = _state.value.copy(editRemoteHost = v) }
    fun setRemotePort(v: String) { _state.value = _state.value.copy(editRemotePort = v.filter { it.isDigit() }) }

    fun save() {
        val s = _state.value
        val lp = s.editLocal.toIntOrNull() ?: return
        val rp = s.editRemotePort.toIntOrNull() ?: return
        if (s.editRemoteHost.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            try {
                saveForward(PortForward(
                    id = s.editId,
                    serverId = serverId,
                    localPort = lp,
                    remoteHost = s.editRemoteHost.trim(),
                    remotePort = rp,
                    enabled = true,
                    createdAt = 0L,
                ))
                _state.value = _state.value.copy(saving = false, showEditDialog = false)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(saving = false, error = t.message)
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { deleteForward(id) }
    }
}
