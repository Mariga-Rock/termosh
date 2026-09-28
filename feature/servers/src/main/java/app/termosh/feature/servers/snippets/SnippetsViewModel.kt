package app.termosh.feature.servers.snippets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.termosh.domain.model.Snippet
import app.termosh.domain.usecase.snippet.ObserveSnippetsUseCase
import app.termosh.domain.usecase.snippet.SaveSnippetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SnippetsViewModel @Inject constructor(
    observeSnippets: ObserveSnippetsUseCase,
    private val saveSnippet: SaveSnippetUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SnippetsUiState())
    val state: StateFlow<SnippetsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeSnippets()
                .catch { t -> _state.value = _state.value.copy(loading = false, error = t.message) }
                .collect { list ->
                    _state.value = _state.value.copy(snippets = list, loading = false, error = null)
                }
        }
    }

    fun openNew() {
        _state.value = _state.value.copy(
            showEditDialog = true,
            editId = "",
            editName = "",
            editCommand = "",
        )
    }

    fun openEdit(s: Snippet) {
        _state.value = _state.value.copy(
            showEditDialog = true,
            editId = s.id,
            editName = s.name,
            editCommand = s.command,
        )
    }

    fun dismiss() { _state.value = _state.value.copy(showEditDialog = false) }
    fun setName(v: String) { _state.value = _state.value.copy(editName = v) }
    fun setCommand(v: String) { _state.value = _state.value.copy(editCommand = v) }

    fun save() {
        val s = _state.value
        if (s.editName.isBlank() || s.editCommand.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true)
            try {
                saveSnippet(
                    Snippet(
                        id = s.editId,
                        name = s.editName.trim(),
                        command = s.editCommand,
                        sortOrder = 0,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
                _state.value = _state.value.copy(saving = false, showEditDialog = false)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(saving = false, error = t.message)
            }
        }
    }
}
