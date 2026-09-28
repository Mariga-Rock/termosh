package app.termosh.feature.settings.backup

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

enum class ExportMode { CONFIG_NO_SECRETS, FULL_BACKUP }

data class BackupUiState(
    val busy: Boolean = false,
    val status: String = "",
    val error: String? = null,
    val showExportPassword: Boolean = false,
    val showImportPassword: Boolean = false,
    val exportMode: ExportMode = ExportMode.CONFIG_NO_SECRETS,
    val password: String = "",
    val passwordConfirm: String = "",
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val manager: BackupManager,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun openExportConfig() {
        _state.value = _state.value.copy(
            showExportPassword = true,
            exportMode = ExportMode.CONFIG_NO_SECRETS,
            password = "",
            passwordConfirm = "",
            error = null,
        )
    }

    fun openExportFull() {
        _state.value = _state.value.copy(
            showExportPassword = true,
            exportMode = ExportMode.FULL_BACKUP,
            password = "",
            passwordConfirm = "",
            error = null,
        )
    }

    fun openImport() {
        _state.value = _state.value.copy(
            showImportPassword = true,
            password = "",
            error = null,
        )
    }

    fun dismiss() {
        _state.value = _state.value.copy(
            showExportPassword = false,
            showImportPassword = false,
            password = "",
            passwordConfirm = "",
        )
    }

    fun setPassword(v: String) { _state.value = _state.value.copy(password = v) }
    fun setPasswordConfirm(v: String) { _state.value = _state.value.copy(passwordConfirm = v) }

    fun exportTo(uri: Uri) {
        val s = _state.value
        val withSecrets = s.exportMode == ExportMode.FULL_BACKUP
        if (withSecrets) {
            if (s.password.isBlank()) return
            if (s.password != s.passwordConfirm) {
                _state.value = s.copy(error = "Пароли не совпадают")
                return
            }
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null, status = "Экспорт...")
            try {
                val data = manager.export(includeSecrets = withSecrets)
                val json = data.toJson()
                val out = if (withSecrets) BackupCrypto.encrypt(json, s.password.toCharArray()) else json
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(out.toByteArray(Charsets.UTF_8))
                    } ?: error("Не удалось открыть файл для записи")
                }
                val msg = if (withSecrets) {
                    "Экспортировано: ${data.servers.size} серверов, ${data.keys.size} ключей (с секретами)"
                } else {
                    "Экспортировано: ${data.servers.size} серверов, ${data.snippets.size} сниппетов (без секретов)"
                }
                _state.value = _state.value.copy(
                    busy = false,
                    status = msg,
                    showExportPassword = false,
                    password = "",
                    passwordConfirm = "",
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    busy = false,
                    error = t.message ?: "Export failed",
                    status = "",
                )
            }
        }
    }

    fun importFrom(uri: Uri) {
        val password = _state.value.password
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null, status = "Импорт...")
            try {
                val raw = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    } ?: error("Не удалось открыть файл")
                }
                val trimmed = raw.trim()
                val isEncrypted = !trimmed.startsWith("{")
                val json: String
                val secretsAvailable: Boolean
                if (isEncrypted) {
                    if (password.isBlank()) error("Файл зашифрован, введите пароль")
                    json = BackupCrypto.decrypt(trimmed, password.toCharArray())
                    secretsAvailable = true
                } else {
                    json = trimmed
                    secretsAvailable = false
                }
                val data = BackupData.fromJson(json)
                val stats = manager.import(data, secretsAvailable)
                val msg = buildString {
                    append("Импортировано: ${stats.serversImported} серверов")
                    if (stats.keysImported > 0) append(", ${stats.keysImported} ключей")
                    if (stats.snippetsImported > 0) append(", ${stats.snippetsImported} сниппетов")
                    append(".")
                    if (!stats.includesSecrets || stats.serversWithoutAuth > 0) {
                        append("\n\nПароли и ключи не входят в этот файл. ")
                        append("Введите их при первом подключении или импортируйте отдельно.")
                    } else {
                        append("\n\nВсе секреты перешифрованы мастер-ключом этого устройства.")
                    }
                }
                _state.value = _state.value.copy(
                    busy = false,
                    status = msg,
                    showImportPassword = false,
                    password = "",
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    busy = false,
                    error = t.message ?: "Import failed",
                    status = "",
                )
            }
        }
    }

    fun clearStatus() {
        _state.value = _state.value.copy(status = "", error = null)
    }
}
