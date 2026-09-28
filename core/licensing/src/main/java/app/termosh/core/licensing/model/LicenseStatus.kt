package app.termosh.core.licensing.model

sealed interface LicenseStatus {
    data object NotActivated : LicenseStatus
    data class Activated(val license: License) : LicenseStatus
    data class Invalid(val reason: String) : LicenseStatus
}
