package app.termosh.feature.servers.keys

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.termosh.domain.model.SshKey
import app.termosh.domain.model.SshKeyType
import app.termosh.domain.usecase.key.DeleteKeyUseCase
import app.termosh.domain.usecase.key.GenerateKeyUseCase
import app.termosh.domain.usecase.key.ObserveKeysUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeysViewModel @Inject constructor(
    observeKeys: ObserveKeysUseCase,
    private val generateKey: GenerateKeyUseCase,
    private val deleteKey: DeleteKeyUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(KeysUiState())
    val state: StateFlow<KeysUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeKeys()
                .catch { t -> _state.value = _state.value.copy(loading = false, error = t.message) }
                .collect { list ->
                    _state.value = _state.value.copy(keys = list, loading = false, error = null)
                }
        }
    }

    fun openGenerate() { _state.value = _state.value.copy(showGenerateDialog = true) }
    fun dismissGenerate() { _state.value = _state.value.copy(showGenerateDialog = false, generating = false) }
    fun setType(t: SshKeyType) { _state.value = _state.value.copy(generateType = t) }
    fun setComment(c: String) { _state.value = _state.value.copy(generateComment = c) }

    fun generate() {
        val s = _state.value
        if (s.generating) return
        _state.value = s.copy(generating = true, error = null)
        viewModelScope.launch {
            try {
                generateKey(s.generateType, s.generateComment.ifBlank { null })
                _state.value = _state.value.copy(
                    generating = false,
                    showGenerateDialog = false,
                    generateComment = "",
                    generateType = SshKeyType.ED25519,
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(generating = false, error = t.message ?: "Generate failed")
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { deleteKey(id) }
    }

    fun showPublic(key: SshKey) { _state.value = _state.value.copy(showPublicKeyFor = key) }
    fun hidePublic() { _state.value = _state.value.copy(showPublicKeyFor = null) }

    fun copyToClipboard(text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Termosh public key", text))
    }
}
