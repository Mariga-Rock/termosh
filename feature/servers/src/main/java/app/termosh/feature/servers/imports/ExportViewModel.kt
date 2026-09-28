package app.termosh.feature.servers.imports

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.termosh.core.ssh.imports.OpenSshConfigWriter
import app.termosh.domain.repository.ServerRepository
import app.termosh.domain.repository.SshKeyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ExportUiState(
    val busy: Boolean = false,
    val preview: String = "",
    val status: String = "",
    val error: String? = null,
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val servers: ServerRepository,
    private val keys: SshKeyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ExportUiState())
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

    init { prepare() }

    fun prepare() {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null)
            try {
                val text = withContext(Dispatchers.IO) {
                    val serverList = servers.observeAll().first()
                    val keyList = keys.observeAll().first()
                    val keyMap = keyList.associateBy { it.id }
                    val nameMap = serverList.associate { it.id to it.name }
                    OpenSshConfigWriter.write(serverList, keyMap, nameMap)
                }
                _state.value = _state.value.copy(busy = false, preview = text)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(busy = false, error = t.message ?: "Export failed")
            }
        }
    }

    fun writeTo(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null, status = "")
            try {
                withContext(Dispatchers.IO) {
                    val text = _state.value.preview
                    if (text.isBlank()) error("Нет данных для экспорта")
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(text.toByteArray(Charsets.UTF_8))
                    } ?: error("Не удалось открыть выходной файл")
                }
                _state.value = _state.value.copy(busy = false, status = "Сохранено")
            } catch (t: Throwable) {
                _state.value = _state.value.copy(busy = false, error = t.message ?: "Export failed")
            }
        }
    }
}
