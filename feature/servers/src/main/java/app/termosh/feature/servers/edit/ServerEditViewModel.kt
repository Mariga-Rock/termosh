package app.termosh.feature.servers.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.termosh.domain.model.Server
import app.termosh.domain.model.ServerAuth
import app.termosh.domain.repository.SecretCodec
import app.termosh.domain.usecase.key.ObserveKeysUseCase
import app.termosh.domain.usecase.server.GetServerUseCase
import app.termosh.domain.usecase.server.SaveServerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val getServer: GetServerUseCase,
    private val saveServer: SaveServerUseCase,
    private val deleteServer: app.termosh.domain.usecase.server.DeleteServerUseCase,
    observeKeys: ObserveKeysUseCase,
    observeServers: app.termosh.domain.usecase.server.ObserveServersUseCase,
    observeTotp: app.termosh.domain.usecase.totp.ObserveTotpUseCase,
    private val codec: SecretCodec,
) : ViewModel() {

    private val serverId: String = savedState.get<String>("serverId") ?: ""

    private val _state = MutableStateFlow(ServerEditUiState())
    val state: StateFlow<ServerEditUiState> = _state.asStateFlow()

    private var existingAuth: ServerAuth? = null

    init {
        viewModelScope.launch {
            observeKeys().collect { list ->
                _state.value = _state.value.copy(availableKeys = list)
            }
        }
        viewModelScope.launch {
            observeServers().collect { list ->
                val filtered = list.filter { it.id != serverId }
                _state.value = _state.value.copy(availableServers = filtered)
            }
        }
        viewModelScope.launch {
            observeTotp().collect { list ->
                _state.value = _state.value.copy(availableTotp = list)
            }
        }
        if (serverId.isNotBlank()) {
            viewModelScope.launch { load(serverId) }
        } else {
            _state.value = _state.value.copy(isNew = true)
        }
    }

    private suspend fun load(id: String) {
        val server = getServer(id)
        existingAuth = server.auth
        _state.value = _state.value.copy(
            id = server.id,
            name = server.name,
            host = server.host,
            port = server.port.toString(),
            username = server.username,
            tags = server.tags.joinToString(", "),
            proxyJumpId = server.proxyJumpId,
            useMosh = server.useMosh,
            totpSecretId = server.totpSecretId,
            useJumpCredentials = server.useJumpCredentials,
            startupCommandsText = server.startupCommands.joinToString("\n"),
            envVarsText = server.envVars.entries.joinToString("\n") { "${it.key}=${it.value}" },
            envSecretsText = server.envSecrets.keys.joinToString("\n") { key ->
                val plain = codec.decryptString(server.envSecrets[key]!!)
                "$key=$plain"
            },
            authMode = when (server.auth) {
                is ServerAuth.Password -> AuthMode.PASSWORD
                is ServerAuth.Keys -> AuthMode.KEY
            },
            password = "",
            selectedKeyIds = (server.auth as? ServerAuth.Keys)?.keyIds ?: emptyList(),
            isNew = false,
        )
    }

    fun setName(v: String) = update { it.copy(name = v) }
    fun setHost(v: String) = update { it.copy(host = v) }
    fun setPort(v: String) = update { it.copy(port = v.filter { c -> c.isDigit() }) }
    fun setUsername(v: String) = update { it.copy(username = v) }
    fun setTags(v: String) = update { it.copy(tags = v) }
    fun setProxyJump(id: String?) = update { it.copy(proxyJumpId = id) }
    fun setUseMosh(v: Boolean) = update { it.copy(useMosh = v) }
    fun setTotpSecret(id: String?) = update { it.copy(totpSecretId = id) }
    fun setUseJumpCredentials(v: Boolean) = update { it.copy(useJumpCredentials = v) }
    fun setStartupCommandsText(v: String) = update { it.copy(startupCommandsText = v) }
    fun setEnvVarsText(v: String) = update { it.copy(envVarsText = v) }
    fun setEnvSecretsText(v: String) = update { it.copy(envSecretsText = v) }
    fun setPassword(v: String) = update { it.copy(password = v) }
    fun toggleKey(id: String) {
        val cur = _state.value.selectedKeyIds
        val next = if (id in cur) cur - id else cur + id
        update { it.copy(selectedKeyIds = next) }
    }
    fun setKeySelection(ids: List<String>) = update { it.copy(selectedKeyIds = ids) }
    fun setAuthMode(mode: AuthMode) = update { it.copy(authMode = mode) }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            update { it.copy(saving = true, error = null) }
            try {
                val auth: ServerAuth = when (s.authMode) {
                    AuthMode.PASSWORD -> {
                        if (s.password.isNotBlank()) ServerAuth.Password(codec.encryptString(s.password))
                        else existingAuth ?: error("No password provided")
                    }
                    AuthMode.KEY -> ServerAuth.Keys(s.selectedKeyIds.ifEmpty { error("No keys selected") })
                }
                val tags = s.tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                val server = Server(
                    id = s.id,
                    name = s.name.trim(),
                    host = s.host.trim(),
                    port = s.port.toIntOrNull() ?: 22,
                    username = s.username.trim(),
                    auth = auth,
                    proxyJumpId = s.proxyJumpId,
                    tags = tags,
                    createdAt = System.currentTimeMillis(),
                    lastUsedAt = null,
                    useMosh = s.useMosh,
                    totpSecretId = s.totpSecretId,
                    useJumpCredentials = s.useJumpCredentials,
                    startupCommands = parseStartup(s.startupCommandsText),
                    envVars = parseEnvVars(s.envVarsText),
                    envSecrets = parseEnvSecrets(s.envSecretsText),
                )
                saveServer(server)
                update { it.copy(saving = false, finished = true) }
            } catch (t: Throwable) {
                update { it.copy(saving = false, error = t.message ?: "Save failed") }
            }
        }
    }

    fun delete() {
        val id = _state.value.id
        if (id.isBlank()) return
        viewModelScope.launch {
            try {
                deleteServer(id)
                update { it.copy(finished = true) }
            } catch (t: Throwable) {
                update { it.copy(error = t.message ?: "Delete failed") }
            }
        }
    }

    private inline fun update(block: (ServerEditUiState) -> ServerEditUiState) {
        _state.value = block(_state.value)
    }

    private fun parseStartup(text: String): List<String> =
        text.lines().map { it.trim() }.filter { it.isNotEmpty() }

    private fun parseEnvVars(text: String): Map<String, String> =
        text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.contains('=') }
            .associate { line ->
                val idx = line.indexOf('=')
                val k = line.substring(0, idx).trim()
                val v = line.substring(idx + 1)
                k to v
            }

    private fun parseEnvSecrets(text: String): Map<String, app.termosh.domain.model.SecretRef> =
        text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.contains('=') }
            .associate { line ->
                val idx = line.indexOf('=')
                val k = line.substring(0, idx).trim()
                val v = line.substring(idx + 1)
                k to codec.encryptString(v)
            }

}
