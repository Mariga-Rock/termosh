package app.termosh.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.termosh.core.datastore.SettingsDataStore
import app.termosh.core.licensing.LicenseRepository
import app.termosh.core.licensing.model.LicenseStatus
import app.termosh.core.ui.theme.TermoshThemeOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val theme: TermoshThemeOption = TermoshThemeOption.TOKYO_NIGHT,
    val logSessions: Boolean = false,
    val notifyOnFinish: Boolean = false,
    val isPro: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsDataStore,
    licenseRepository: LicenseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            licenseRepository.status.collect { st ->
                val pro = st is LicenseStatus.Activated
                _state.value = _state.value.copy(isPro = pro)
            }
        }
        viewModelScope.launch {
            store.theme.collect { name ->
                val t = runCatching { TermoshThemeOption.valueOf(name) }
                    .getOrDefault(TermoshThemeOption.TOKYO_NIGHT)
                _state.value = _state.value.copy(theme = t)
            }
        }
        viewModelScope.launch {
            store.logSessions.collect { v ->
                _state.value = _state.value.copy(logSessions = v)
            }
        }
        viewModelScope.launch {
            store.notifyOnFinish.collect { v ->
                _state.value = _state.value.copy(notifyOnFinish = v)
            }
        }
    }

    fun setTheme(t: TermoshThemeOption) {
        viewModelScope.launch { store.setTheme(t.name) }
    }

    fun setLogSessions(v: Boolean) {
        viewModelScope.launch { store.setLogSessions(v) }
    }
    fun setNotifyOnFinish(v: Boolean) {
        viewModelScope.launch { store.setNotifyOnFinish(v) }
    }
}
