package app.termosh.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "termosh_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val logSessions = booleanPreferencesKey("log_sessions")
        val notifyOnFinish = booleanPreferencesKey("notify_on_finish")
    }

    val theme: Flow<String> = context.dataStore.data.map { it[Keys.theme] ?: "TOKYO_NIGHT" }
    val logSessions: Flow<Boolean> = context.dataStore.data.map { it[Keys.logSessions] ?: false }
    val notifyOnFinish: Flow<Boolean> = context.dataStore.data.map { it[Keys.notifyOnFinish] ?: false }

    suspend fun setTheme(name: String) {
        context.dataStore.edit { it[Keys.theme] = name }
    }

    suspend fun setLogSessions(v: Boolean) {
        context.dataStore.edit { it[Keys.logSessions] = v }
    }

    suspend fun setNotifyOnFinish(v: Boolean) {
        context.dataStore.edit { it[Keys.notifyOnFinish] = v }
    }
}
