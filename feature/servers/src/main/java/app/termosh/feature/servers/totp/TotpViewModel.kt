package app.termosh.feature.servers.totp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.termosh.core.security.TotpGenerator
import app.termosh.domain.model.SecretRef
import app.termosh.domain.model.TotpSecret
import app.termosh.domain.repository.SecretCodec
import app.termosh.domain.usecase.totp.DeleteTotpUseCase
import app.termosh.domain.usecase.totp.ObserveTotpUseCase
import app.termosh.domain.usecase.totp.SaveTotpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TotpItem(
    val id: String,
    val label: String,
    val issuer: String,
    val currentCode: String,
    val secondsLeft: Int,
)

data class TotpUiState(
    val items: List<TotpItem> = emptyList(),
    val loading: Boolean = true,
    val showEditDialog: Boolean = false,
    val editId: String = "",
    val editLabel: String = "",
    val editIssuer: String = "",
    val editSecret: String = "",
    val error: String? = null,
)

@HiltViewModel
class TotpViewModel @Inject constructor(
    private val observeTotp: ObserveTotpUseCase,
    private val saveTotp: SaveTotpUseCase,
    private val deleteTotp: DeleteTotpUseCase,
    private val codec: SecretCodec,
    private val totpGen: TotpGenerator,
) : ViewModel() {

    private val _state = MutableStateFlow(TotpUiState())
    val state: StateFlow<TotpUiState> = _state.asStateFlow()

    private var rawSecrets: List<Pair<TotpSecret, ByteArray>> = emptyList()

    init {
        viewModelScope.launch {
            observeTotp().collect { list ->
                rawSecrets = list.mapNotNull { s ->
                    val bytes = runCatching { codec.decrypt(s.secretRef) }.getOrNull()
                    if (bytes != null) s to bytes else null
                }
                updateCodes()
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (rawSecrets.isNotEmpty()) updateCodes()
            }
        }
    }

    private fun updateCodes() {
        val items = rawSecrets.map { (s, bytes) ->
            TotpItem(
                id = s.id,
                label = s.label,
                issuer = s.issuer,
                currentCode = totpGen.generate(bytes, digits = s.digits, periodSec = s.periodSec, algorithm = s.algorithm),
                secondsLeft = totpGen.secondsRemaining(s.periodSec),
            )
        }
        _state.value = _state.value.copy(items = items, loading = false)
    }

    fun openNew() { _state.value = _state.value.copy(showEditDialog = true, editId = "", editLabel = "", editIssuer = "", editSecret = "", error = null) }
    fun openEdit(item: TotpItem) {
        val s = rawSecrets.firstOrNull { it.first.id == item.id }?.first ?: return
        _state.value = _state.value.copy(
            showEditDialog = true,
            editId = s.id,
            editLabel = s.label,
            editIssuer = s.issuer,
            editSecret = "",
            error = null,
        )
    }
    fun dismiss() { _state.value = _state.value.copy(showEditDialog = false) }
    fun setLabel(v: String) { _state.value = _state.value.copy(editLabel = v) }
    fun setIssuer(v: String) { _state.value = _state.value.copy(editIssuer = v) }
    fun setSecret(v: String) { _state.value = _state.value.copy(editSecret = v) }

    fun save() {
        val s = _state.value
        if (s.editLabel.isBlank()) return
        viewModelScope.launch {
            try {
                val plainBytes: ByteArray = if (s.editSecret.isNotBlank()) {
                    totpGen.base32Decode(s.editSecret)
                } else {
                    rawSecrets.firstOrNull { it.first.id == s.editId }?.second
                        ?: throw IllegalStateException("Secret required")
                }
                val ref: SecretRef = codec.encrypt(plainBytes)
                saveTotp(
                    TotpSecret(
                        id = s.editId,
                        label = s.editLabel.trim(),
                        issuer = s.editIssuer.trim(),
                        secretRef = ref,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
                _state.value = _state.value.copy(showEditDialog = false)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(error = t.message ?: "Save failed")
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { deleteTotp(id) }
    }
}
