package app.termosh.feature.servers.snippets

import app.termosh.domain.model.Snippet

data class SnippetsUiState(
    val snippets: List<Snippet> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val showEditDialog: Boolean = false,
    val editId: String = "",
    val editName: String = "",
    val editCommand: String = "",
    val saving: Boolean = false,
)
