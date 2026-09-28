package app.termosh.feature.servers.edit

import app.termosh.domain.model.SshKey

data class ServerEditUiState(
    val id: String = "",
    val name: String = "",
    val host: String = "",
    val port: String = "22",
    val username: String = "",
    val tags: String = "",
    val proxyJumpId: String? = null,
    val useMosh: Boolean = false,
    val totpSecretId: String? = null,
    val useJumpCredentials: Boolean = false,
    val availableTotp: List<app.termosh.domain.model.TotpSecret> = emptyList(),
    val startupCommandsText: String = "",
    val envVarsText: String = "",
    val envSecretsText: String = "",
    val availableServers: List<app.termosh.domain.model.Server> = emptyList(),
    val authMode: AuthMode = AuthMode.PASSWORD,
    val password: String = "",
    val selectedKeyIds: List<String> = emptyList(),
    val availableKeys: List<SshKey> = emptyList(),
    val isNew: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val finished: Boolean = false,
) {
    val canSave: Boolean
        get() = name.isNotBlank() &&
            host.isNotBlank() &&
            username.isNotBlank() &&
            (port.isBlank() || port.toIntOrNull()?.let { it in 1..65535 } == true) &&
            when (authMode) {
                AuthMode.PASSWORD -> password.isNotBlank() || !isNew
                AuthMode.KEY -> selectedKeyIds.isNotEmpty()
            }
}

enum class AuthMode { PASSWORD, KEY }
