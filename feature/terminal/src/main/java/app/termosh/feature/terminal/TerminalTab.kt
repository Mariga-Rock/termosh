package app.termosh.feature.terminal

data class TerminalTab(
    val tabId: String,
    val serverId: String,
    val name: String,
    val isMosh: Boolean = false,
)
