package app.termosh.core.licensing

import app.termosh.core.licensing.model.License
import app.termosh.core.licensing.model.LicenseStatus
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicenseVerifier @Inject constructor(
    private val device: DeviceIdentityProvider,
) {

    fun verify(code: String): LicenseStatus {
        val trimmed = code.trim().replace("\n", "").replace(" ", "")
        val dot = trimmed.indexOf('.')
        if (dot <= 0 || dot >= trimmed.length - 1) return LicenseStatus.Invalid("Неверный формат кода")

        val jsonB64 = trimmed.substring(0, dot)
        val sigB64 = trimmed.substring(dot + 1)

        val licenseJson = try {
            String(Base64.getUrlDecoder().decode(jsonB64))
        } catch (t: Throwable) {
            return LicenseStatus.Invalid("Не удалось декодировать лицензию")
        }

        val license = try {
            License.decode(licenseJson)
        } catch (t: Throwable) {
            return LicenseStatus.Invalid("Ошибка в структуре лицензии")
        }

        if (OwnerPublicKey.BASE64.isBlank()) {
            return LicenseStatus.Invalid("Публичный ключ владельца не встроен")
        }

        val signatureBytes = try {
            Base64.getUrlDecoder().decode(sigB64)
        } catch (t: Throwable) {
            return LicenseStatus.Invalid("Не удалось декодировать подпись")
        }

        val pubKeyBytes = try {
            Base64.getDecoder().decode(OwnerPublicKey.BASE64)
        } catch (t: Throwable) {
            return LicenseStatus.Invalid("Публичный ключ владельца повреждён")
        }

        val canonical = license.encodeCanonical()
        val ok = verifyEd25519(pubKeyBytes, canonical.toByteArray(Charsets.UTF_8), signatureBytes)
        if (!ok) return LicenseStatus.Invalid("Подпись не совпадает")

        val expectedPub = device.publicKeyBase64()
        if (license.devicePublicKeyBase64 != expectedPub) {
            return LicenseStatus.Invalid("Лицензия привязана к другому устройству")
        }

        val now = System.currentTimeMillis()
        if (license.expiresAt != null && now > license.expiresAt) {
            return LicenseStatus.Invalid("Срок лицензии истёк")
        }

        return LicenseStatus.Activated(license)
    }

    private fun verifyEd25519(pubKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        return try {
            val raw = if (pubKey.size == 32) pubKey else pubKey.copyOfRange(pubKey.size - 32, pubKey.size)
            val params = Ed25519PublicKeyParameters(raw, 0)
            val signer = Ed25519Signer()
            signer.init(false, params)
            signer.update(message, 0, message.size)
            signer.verifySignature(signature)
        } catch (t: Throwable) {
            false
        }
    }
}
