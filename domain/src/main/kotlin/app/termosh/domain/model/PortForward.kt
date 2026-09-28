package app.termosh.domain.model

data class PortForward(
    val id: String,
    val serverId: String,
    val localPort: Int,
    val remoteHost: String,
    val remotePort: Int,
    val enabled: Boolean,
    val createdAt: Long,
)
