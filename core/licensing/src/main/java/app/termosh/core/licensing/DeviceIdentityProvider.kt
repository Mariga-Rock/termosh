package app.termosh.core.licensing

import android.content.Context
import android.os.Build
import app.termosh.core.licensing.model.DeviceRequest
import app.termosh.core.security.SshKeyStore
import app.termosh.core.security.model.SshKeyType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdentityProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keyStore: SshKeyStore,
) {
    private val prefs by lazy {
        context.getSharedPreferences("termosh_device_id", Context.MODE_PRIVATE)
    }

    fun deviceUuid(): String {
        var u = prefs.getString("uuid", null)
        if (u == null) {
            u = UUID.randomUUID().toString()
            prefs.edit().putString("uuid", u).apply()
        }
        return u
    }

    fun publicKeyBase64(): String {
        var pk = prefs.getString("pubkey", null)
        if (pk == null) {
            val pair = keyStore.generateAndWrap(SshKeyType.ED25519, "termosh-device")
            val parts = pair.publicKeyOpenSsh.split(' ')
            pk = parts.getOrNull(1) ?: ""
            prefs.edit().putString("pubkey", pk).apply()
        }
        return pk
    }

    fun buildRequest(): DeviceRequest = DeviceRequest(
        uuid = deviceUuid(),
        publicKeyBase64 = publicKeyBase64(),
        label = "${Build.MANUFACTURER} ${Build.MODEL}",
    )
}
