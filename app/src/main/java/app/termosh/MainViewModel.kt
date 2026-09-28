package app.termosh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.termosh.core.datastore.SettingsDataStore
import app.termosh.core.ui.theme.TermoshThemeOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    store: SettingsDataStore,
) : ViewModel() {

    val theme: StateFlow<TermoshThemeOption> = store.theme
        .map { name ->
            runCatching { TermoshThemeOption.valueOf(name) }
                .getOrDefault(TermoshThemeOption.TOKYO_NIGHT)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = TermoshThemeOption.TOKYO_NIGHT,
        )
}
