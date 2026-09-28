package app.termosh.core.ssh.verifier

import net.schmizz.sshj.transport.verification.HostKeyVerifier
import java.security.MessageDigest
import java.security.PublicKey
import java.util.Base64

/**
 * Trust-On-First-Use проверка ключа хоста.
 *
 * @param knownFingerprint SHA256-fingerprint, если хост уже известен, или null.
 * @param onFirstUse       вызывается при первом подключении — сохранить fingerprint.
 *
 * Возвращает true, если совпало или это первый раз; false — если fingerprint не совпал.
 * При false sshj разорвёт соединение.
 */
class TofuHostKeyVerifier(
    private val knownFingerprint: String?,
    private val onFirstUse: (String) -> Unit,
) : HostKeyVerifier {

    override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
        val actual = sha256(key)
        return if (knownFingerprint == null) {
            onFirstUse(actual)
            true
        } else {
            knownFingerprint == actual
        }
    }

    override fun findExistingAlgorithms(hostname: String, port: Int): MutableList<String> =
        mutableListOf()

    companion object {
        fun sha256(key: PublicKey): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(key.encoded)
            val b64 = Base64.getEncoder().withoutPadding().encodeToString(digest)
            return "SHA256:$b64"
        }
    }
}
