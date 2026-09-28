package app.termosh.feature.servers.keys

import app.termosh.domain.model.SshKey
import app.termosh.domain.model.SshKeyType

data class KeysUiState(
    val keys: List<SshKey> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val showGenerateDialog: Boolean = false,
    val generateType: SshKeyType = SshKeyType.ED25519,
    val generateComment: String = "",
    val generating: Boolean = false,
    val showPublicKeyFor: SshKey? = null,
)
