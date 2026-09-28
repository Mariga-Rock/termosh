package app.termosh.core.ssh

data class ForwardSpec(
    val localPort: Int,
    val remoteHost: String,
    val remotePort: Int,
)
