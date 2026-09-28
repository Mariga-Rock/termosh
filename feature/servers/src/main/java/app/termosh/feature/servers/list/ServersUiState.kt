package app.termosh.feature.servers.list

import app.termosh.domain.model.Server

enum class ServerSort { MANUAL, NAME, LAST_USED, CREATED }

data class ServersUiState(
    val servers: List<Server> = emptyList(),
    val query: String = "",
    val sort: ServerSort = ServerSort.MANUAL,
    val loading: Boolean = true,
    val error: String? = null,
    val isPro: Boolean = false,
    val freeLimit: Int = 5,
    val showLimitDialog: Boolean = false,
    val forwardsByServer: Map<String, Boolean> = emptyMap(),
) {
    val totalCount: Int get() = servers.size
    val canAdd: Boolean get() = isPro || totalCount < freeLimit
    val reorderEnabled: Boolean get() = sort == ServerSort.MANUAL && query.isBlank()
}
