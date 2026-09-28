package app.termosh.feature.terminal

enum class TerminalConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR, RECONNECTING,
}

data class TerminalUiState(
    val connectionState: TerminalConnectionState = TerminalConnectionState.DISCONNECTED,
    val statusMessage: String = "Disconnected",
    val serverName: String = "",
    val input: String = "",
    val modifiers: Set<String> = emptySet(),
    val retryAttempt: Int = 0,
    val retryMax: Int = 10,
)
