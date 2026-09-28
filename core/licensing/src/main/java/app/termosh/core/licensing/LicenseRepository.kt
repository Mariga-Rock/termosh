package app.termosh.core.licensing

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.termosh.core.licensing.model.LicenseStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.licenseStore by preferencesDataStore(name = "termosh_license")

@Singleton
class LicenseRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val verifier: LicenseVerifier,
) {
    private val codeKey = stringPreferencesKey("code")

    val status: Flow<LicenseStatus> = context.licenseStore.data.map { prefs ->
        val code = prefs[codeKey]
        if (code.isNullOrBlank()) LicenseStatus.NotActivated else verifier.verify(code)
    }

    suspend fun save(code: String): LicenseStatus {
        val status = verifier.verify(code)
        if (status is LicenseStatus.Activated) {
            context.licenseStore.edit { it[codeKey] = code }
        }
        return status
    }

    suspend fun clear() {
        context.licenseStore.edit { it.remove(codeKey) }
    }
}
