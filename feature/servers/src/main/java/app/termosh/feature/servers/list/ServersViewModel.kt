package app.termosh.feature.servers.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.termosh.core.licensing.LicenseRepository
import app.termosh.core.licensing.model.LicenseStatus
import app.termosh.domain.model.Server
import app.termosh.domain.usecase.server.DeleteServerUseCase
import app.termosh.domain.usecase.server.ObserveServersUseCase
import app.termosh.domain.usecase.server.ReorderServersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import app.termosh.domain.repository.PortForwardRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@HiltViewModel
class ServersViewModel @Inject constructor(
    observeServers: ObserveServersUseCase,
    private val deleteServer: DeleteServerUseCase,
    private val reorderServers: ReorderServersUseCase,
    licenseRepository: LicenseRepository,
    private val portForwards: PortForwardRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ServersUiState())
    val state: StateFlow<ServersUiState> = _state.asStateFlow()

    private var allServers: List<Server> = emptyList()

    init {
        viewModelScope.launch {
            observeServers()
                .catch { t ->
                    _state.value = _state.value.copy(loading = false, error = t.message ?: "Unknown error")
                }
                .collect { list ->
                    allServers = list
                    applyFilters()
                }
        }
        viewModelScope.launch {
            observeServers()
                .flatMapLatest { servers: List<Server> ->
                    if (servers.isEmpty()) {
                        kotlinx.coroutines.flow.flowOf(emptyMap<String, Boolean>())
                    } else {
                        val flows: List<kotlinx.coroutines.flow.Flow<Pair<String, Boolean>>> =
                            servers.map { srv: Server ->
                                portForwards.observeForServer(srv.id)
                                    .map { list: List<app.termosh.domain.model.PortForward> ->
                                        srv.id to list.any { pf -> pf.enabled }
                                    }
                            }
                        if (flows.size == 1) {
                            flows[0].map { pair -> mapOf(pair.first to pair.second) }
                        } else {
                            kotlinx.coroutines.flow.combine(flows) { arr ->
                                arr.toMap()
                            }
                        }
                    }
                }
                .collect { map ->
                    _state.value = _state.value.copy(forwardsByServer = map)
                }
        }
        viewModelScope.launch {
            licenseRepository.status.collect { status ->
                val pro = status is LicenseStatus.Activated
                _state.value = _state.value.copy(isPro = pro)
            }
        }
    }

    private fun applyFilters() {
        val s = _state.value
        val q = s.query.trim().lowercase()
        val filtered = if (q.isBlank()) allServers else allServers.filter { srv ->
            srv.name.lowercase().contains(q) ||
                srv.host.lowercase().contains(q) ||
                srv.username.lowercase().contains(q) ||
                srv.tags.any { it.lowercase().contains(q) }
        }
        val sorted = when (s.sort) {
            ServerSort.MANUAL -> filtered.sortedBy { it.name.lowercase() } // порядок задаёт БД по sortOrder
            ServerSort.NAME -> filtered.sortedBy { it.name.lowercase() }
            ServerSort.LAST_USED -> filtered.sortedByDescending { it.lastUsedAt ?: 0L }
            ServerSort.CREATED -> filtered.sortedByDescending { it.createdAt }
        }
        _state.value = s.copy(servers = sorted, loading = false, error = null)
    }

    fun setQuery(v: String) {
        _state.value = _state.value.copy(query = v)
        applyFilters()
    }

    fun setSort(sort: ServerSort) {
        _state.value = _state.value.copy(sort = sort)
        applyFilters()
    }

    fun delete(id: String) {
        viewModelScope.launch { deleteServer(id) }
    }

    fun onAddClicked(onProceed: () -> Unit) {
        if (_state.value.canAdd) onProceed()
        else _state.value = _state.value.copy(showLimitDialog = true)
    }

    fun dismissLimitDialog() {
        _state.value = _state.value.copy(showLimitDialog = false)
    }

    /** Вызывается после drag-and-drop. */
    fun onReorder(newOrder: List<Server>) {
        if (!_state.value.reorderEnabled) return
        allServers = newOrder
        _state.value = _state.value.copy(servers = newOrder)
        viewModelScope.launch {
            reorderServers(newOrder.map { it.id })
        }
    }
}
