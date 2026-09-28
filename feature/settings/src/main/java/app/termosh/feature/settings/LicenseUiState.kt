package app.termosh.feature.settings

data class LicenseUiState(
    val status: String = "Not activated",
    val recipient: String = "",
    val features: List<String> = emptyList(),
    val expiresAt: Long? = null,
    val inputCode: String = "",
    val deviceRequest: String = "",
    val error: String? = null,
    val saving: Boolean = false,
)
