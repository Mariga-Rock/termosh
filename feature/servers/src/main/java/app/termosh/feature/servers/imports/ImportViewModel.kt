package app.termosh.feature.servers.imports

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ImportUiState(
    val busy: Boolean = false,
    val status: String = "",
    val success: Boolean = false,
    val warnings: List<String> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val importer: ConfigImporter,
) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    fun importSshConfig(uri: Uri) = runImport(uri, "ssh_config")
    fun importKnownHosts(uri: Uri) = runImport(uri, "known_hosts")
    fun importConnectBot(uri: Uri) = runImport(uri, "connectbot")

    private fun runImport(uri: Uri, kind: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null, warnings = emptyList(), status = "Чтение файла...")
            try {
                val (text, reader, basePath) = withContext(Dispatchers.IO) {
                    val t = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: error("Не удалось открыть файл")
                    val r = SafIncludeReader.tryCreate(context, uri)
                    val bp = SafIncludeReader.basePathFor(uri)
                    Triple(t, r, bp)
                }
                val res = when (kind) {
                    "ssh_config" -> importer.importSshConfig(text, reader, basePath)
                    "connectbot" -> importer.importConnectBot(text)
                    else -> importer.importKnownHosts(text)
                }
                val anyImported = res.serversImported > 0 || res.knownHostsImported > 0
                val statusText = if (anyImported) {
                    val parts = buildList {
                        if (res.serversImported > 0) {
                            add("${res.serversImported} ${serverWord(res.serversImported)}")
                        }
                        if (res.knownHostsImported > 0) {
                            add("${res.knownHostsImported} known_hosts")
                        }
                    }
                    "Импортировано: " + parts.joinToString(", ")
                } else {
                    "Не найдено ни одного хоста или известного сервера.\n" +
                        "Возможно, файл пустой или формат не распознан."
                }
                _state.value = _state.value.copy(
                    busy = false,
                    status = statusText,
                    success = anyImported,
                    warnings = res.warnings,
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(busy = false, error = t.message ?: "Import failed")
            }
        }
    }

    private fun serverWord(n: Int): String = when {
        n % 10 == 1 && n % 100 != 11 -> "сервер"
        n % 10 in 2..4 && n % 100 !in 12..14 -> "сервера"
        else -> "серверов"
    }

    fun clear() { _state.value = ImportUiState() }
}
