package app.termosh.core.ssh.verifier

import net.schmizz.sshj.transport.verification.HostKeyVerifier
import java.security.PublicKey

/**
 * Trust-On-First-Use: при первом подключении запоминаем ключ,
 * при последующих — сверяем. При несовпадении бросаем исключение.
 */
class TofuHostKeyVerifier(
    private val knownFingerprint: String?,
    private val onFirstUse: (String, String) -> Unit,
    private val fingerprintCalculator: (PublicKey) -> String,
) : HostKeyVerifier {

    override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
        val actual = fingerprintCalculator(key)
        if (knownFingerprint == null) {
            onFirstUse(hostname, actual)
            return true
        }
        return knownFingerprint == actual
    }

    override fun findExistingAlgorithms(hostname: String, port: Int): MutableList<String> =
        mutableListOf()
}
