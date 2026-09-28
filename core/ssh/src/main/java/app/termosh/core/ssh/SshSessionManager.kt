package app.termosh.core.ssh

import app.termosh.core.ssh.model.SshAuthMethod
import app.termosh.core.ssh.model.SshConnectionConfig
import app.termosh.core.ssh.verifier.TofuHostKeyVerifier
import app.termosh.domain.model.KnownHost
import app.termosh.domain.repository.KnownHostRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshSessionManager @Inject constructor(
    private val knownHosts: KnownHostRepository,
    private val totpGenerator: app.termosh.core.security.TotpGenerator,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sessions = mutableMapOf<String, SshSession>()

    @Synchronized
    fun get(id: String): SshSession? = sessions[id]

    @Synchronized
    fun open(id: String, config: SshConnectionConfig, auth: SshAuthMethod, totpSecret: ByteArray? = null): SshSession {
        sessions[id]?.let { return it }
        val verifier = buildTofu(config)
        val s = SshSession(id, config, auth, totpSecret, totpGenerator, verifier, scope)
        sessions[id] = s
        return s
    }

    private fun buildTofu(config: SshConnectionConfig): TofuHostKeyVerifier {
        val known = runBlocking { knownHosts.findMatch(config.host, config.port) }
        val knownFp = known?.fingerprintSha256
        return TofuHostKeyVerifier(
            knownFingerprint = knownFp,
            onFirstUse = { fp ->
                runBlocking {
                    knownHosts.save(
                        KnownHost(
                            host = config.host,
                            port = config.port,
                            fingerprintSha256 = fp,
                            addedAt = System.currentTimeMillis(),
                        ),
                    )
                }
            },
        )
    }

    @Synchronized
    fun close(id: String) {
        sessions.remove(id)?.let { s -> scope.launch { s.disconnect() } }
    }

    @Synchronized
    fun closeAll() {
        sessions.values.forEach { s -> scope.launch { s.disconnect() } }
        sessions.clear()
    }

    @Synchronized
    fun ids(): List<String> = sessions.keys.toList()
}
