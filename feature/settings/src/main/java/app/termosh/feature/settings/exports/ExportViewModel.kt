package app.termosh.feature.settings.exports

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.termosh.core.common.AuditLog
import app.termosh.core.ssh.exports.OpenSshConfigExporter
import app.termosh.core.ssh.imports.ConnectBotImporter
import app.termosh.domain.model.Server
import app.termosh.domain.model.ServerAuth
import app.termosh.domain.model.SshKey
import app.termosh.domain.repository.SecretCodec
import app.termosh.domain.repository.ServerRepository
import app.termosh.domain.repository.SshKeyRepository
import app.termosh.domain.usecase.server.SaveServerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class ExportUiState(
    val busy: Boolean = false,
    val status: String = "",
    val error: String? = null,
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverRepo: ServerRepository,
    private val keyRepo: SshKeyRepository,
    private val saveServer: SaveServerUseCase,
    private val codec: SecretCodec,
) : ViewModel() {

    private val _state = MutableStateFlow(ExportUiState())
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

    fun exportOpenSshConfig(uri: Uri) {
        viewModelScope.launch {
            _state.value = ExportUiState(busy = true, status = "Экспорт...")
            try {
                val servers = serverRepo.observeAll().first()
                val keys = keyRepo.observeAll().first()
                val serversById = servers.associateBy { it.id }
                val keysById: Map<String, SshKey> = keys.associateBy { it.id }

                val text = OpenSshConfigExporter.export(servers, keysById, serversById)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(text.toByteArray(Charsets.UTF_8))
                    } ?: error("Не удалось открыть файл")
                }
                AuditLog.log(
                    event = "export_openssh_config",
                    fields = mapOf("servers" to servers.size),
                )
                _state.value = ExportUiState(
                    busy = false,
                    status = "Экспортировано ${servers.size} серверов в OpenSSH config",
                )
            } catch (t: Throwable) {
                _state.value = ExportUiState(busy = false, error = t.message ?: "Ошибка")
            }
        }
    }

    fun importConnectBot(uri: Uri) {
        viewModelScope.launch {
            _state.value = ExportUiState(busy = true, status = "Импорт из ConnectBot...")
            try {
                val xml = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    } ?: error("Не удалось открыть файл")
                }
                val hosts = ConnectBotImporter.parse(xml)
                var imported = 0
                var withPassword = 0
                hosts.forEach { h ->
                    val pwd: String? = h.passwordPlain
                    val auth: ServerAuth = if (pwd != null) {
                        withPassword++
                        ServerAuth.Password(codec.encryptString(pwd))
                    } else {
                        ServerAuth.Password(codec.encryptString(""))
                    }
                    saveServer(
                        Server(
                            id = UUID.randomUUID().toString(),
                            name = h.nickname,
                            host = h.hostname,
                            port = h.port,
                            username = h.username,
                            auth = auth,
                            proxyJumpId = null,
                            tags = listOf("connectbot"),
                            createdAt = System.currentTimeMillis(),
                            lastUsedAt = null,
                        )
                    )
                    imported++
                }
                AuditLog.log(
                    event = "import_connectbot",
                    fields = mapOf(
                        "hosts" to imported,
                        "withPassword" to withPassword,
                    ),
                )
                _state.value = ExportUiState(
                    busy = false,
                    status = "Импортировано $imported серверов из ConnectBot (с паролем: $withPassword)",
                )
            } catch (t: Throwable) {
                _state.value = ExportUiState(busy = false, error = t.message ?: "Ошибка")
            }
        }
    }

    fun clear() { _state.value = ExportUiState() }
}
