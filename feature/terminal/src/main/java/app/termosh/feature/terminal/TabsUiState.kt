package app.termosh.feature.terminal

import app.termosh.domain.model.Server
import app.termosh.domain.model.Snippet

data class TabsUiState(
    val tabs: List<TerminalTab> = emptyList(),
    val activeTabId: String? = null,
    val connectionState: TerminalConnectionState = TerminalConnectionState.DISCONNECTED,
    val statusMessage: String = "",
    val input: String = "",
    val pendingInput: String? = null,
    val modifiers: Set<String> = emptySet(),
    val retryAttempt: Int = 0,
    val retryMax: Int = 10,
    val showPicker: Boolean = false,
    val availableServers: List<Server> = emptyList(),
    val snippets: List<Snippet> = emptyList(),
    val showSnippets: Boolean = false,
    val isPro: Boolean = false,
    val freeTabLimit: Int = 3,
    val showTabLimitDialog: Boolean = false,
    val splitTabId: String? = null,
    val splitOrientationVertical: Boolean = true,
    val splitActivePane: Int = 0,
    val tabStatuses: Map<String, TabStatus> = emptyMap(),
)

data class TabStatus(
    val state: TerminalConnectionState,
    val message: String,
)
