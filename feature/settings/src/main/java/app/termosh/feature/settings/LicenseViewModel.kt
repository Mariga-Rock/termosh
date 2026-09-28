package app.termosh.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.termosh.core.licensing.DeviceIdentityProvider
import app.termosh.core.licensing.LicenseRepository
import app.termosh.core.licensing.model.LicenseStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LicenseViewModel @Inject constructor(
    private val repo: LicenseRepository,
    private val device: DeviceIdentityProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(LicenseUiState())
    val state: StateFlow<LicenseUiState> = _state.asStateFlow()

    init {
        _state.value = _state.value.copy(
            deviceRequest = device.buildRequest().encode(),
        )
        viewModelScope.launch {
            repo.status.collect { st -> applyStatus(st) }
        }
    }

    private fun applyStatus(st: LicenseStatus) {
        when (st) {
            is LicenseStatus.NotActivated -> _state.value = _state.value.copy(
                status = "Не активировано",
                recipient = "",
                features = emptyList(),
                expiresAt = null,
            )
            is LicenseStatus.Activated -> _state.value = _state.value.copy(
                status = "Активировано",
                recipient = st.license.recipientName,
                features = st.license.features,
                expiresAt = st.license.expiresAt,
                error = null,
            )
            is LicenseStatus.Invalid -> _state.value = _state.value.copy(
                status = "Ошибка",
                error = st.reason,
            )
        }
    }

    fun setInput(v: String) { _state.value = _state.value.copy(inputCode = v) }

    fun activate() {
        val s = _state.value
        if (s.inputCode.isBlank() || s.saving) return
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            val st = repo.save(s.inputCode.trim())
            _state.value = _state.value.copy(
                saving = false,
                inputCode = "",
                error = if (st is LicenseStatus.Invalid) st.reason else null,
            )
        }
    }

    fun deactivate() {
        viewModelScope.launch {
            repo.clear()
            _state.value = _state.value.copy(status = "Не активировано")
        }
    }
}
