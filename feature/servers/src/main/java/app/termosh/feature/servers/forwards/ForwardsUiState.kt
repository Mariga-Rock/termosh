package app.termosh.feature.servers.forwards

import app.termosh.domain.model.PortForward

data class ForwardsUiState(
    val serverId: String = "",
    val serverName: String = "",
    val forwards: List<PortForward> = emptyList(),
    val loading: Boolean = true,
    val showEditDialog: Boolean = false,
    val editId: String = "",
    val editLocal: String = "8080",
    val editRemoteHost: String = "127.0.0.1",
    val editRemotePort: String = "80",
    val saving: Boolean = false,
    val error: String? = null,
)
