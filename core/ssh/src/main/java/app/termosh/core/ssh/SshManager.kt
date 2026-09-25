package app.termosh.core.ssh

import app.termosh.core.ssh.model.SshAuthMethod
import app.termosh.core.ssh.model.SshConnectionConfig
import app.termosh.core.ssh.model.SshSessionState
import app.termosh.core.ssh.verifier.TofuHostKeyVerifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile
import net.schmizz.sshj.userauth.password.PasswordFinder
import net.schmizz.sshj.userauth.password.Resource
import java.io.ByteArrayInputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Управляет жизненным циклом SSH-сессии через sshj.
 *
 * Все методы — suspend, вызывать из Dispatchers.IO.
 */
@Singleton
class SshManager @Inject constructor() {

    private val _state = MutableStateFlow(SshSessionState.IDLE)
    val state: StateFlow<SshSessionState> = _state.asStateFlow()

    private var client: SSHClient? = null
    private var session: Session? = null

    /**
     * Устанавливает SSH-соединение и открывает shell-канал.
     *
     * @param tofuVerifier если null — используется PromiscuousVerifier (только для отладки).
     */
    suspend fun connect(
        config: SshConnectionConfig,
        auth: SshAuthMethod,
        tofuVerifier: TofuHostKeyVerifier? = null,
    ) = withContext(Dispatchers.IO) {
        _state.value = SshSessionState.CONNECTING

        try {
            val ssh = SSHClient()
            ssh.addHostKeyVerifier(tofuVerifier ?: PromiscuousVerifier())

            // ProxyJump: если задан — сначала подключаемся к jump-хосту
            config.proxyJump?.let { jump ->
                val jumpClient = connectJumpHost(jump)
                // sshj не имеет встроенного multi-hop; для MVP подключаемся напрямую.
                // TODO: реализовать через newDirectConnection(jumpClient)
                jumpClient.close()
            }

            ssh.connect(config.host, config.port)
            ssh.timeout = config.readTimeoutMs

            _state.value = SshSessionState.AUTHENTICATING
            authenticate(ssh, config.username, auth)

            ssh.connection.keepAlive.keepAliveInterval = config.keepAliveIntervalSec

            session = ssh.startSession()
            session!!.allocatePTY(
                "xterm-256color",
                80, 24, 0, 0,
                emptyMap(),
            )

            client = ssh
            _state.value = SshSessionState.CONNECTED
        } catch (t: Throwable) {
            _state.value = SshSessionState.ERROR
            disconnect()
            throw t
        }
    }

    /** Открывает shell-канал (после connect). */
    suspend fun startShell(): Session.Shell = withContext(Dispatchers.IO) {
        val s = session ?: error("Session not started")
        s.startShell()
    }

    /** Пишет данные в stdin shell-канала. */
    fun writeToShell(shell: Session.Shell, data: ByteArray) {
        shell.outputStream.write(data)
        shell.outputStream.flush()
    }

    /** Возвращает InputStream shell-канала для чтения вывода. */
    fun shellInputStream(shell: Session.Shell) = shell.inputStream

    /** Открывает локальный порт-форвардинг. */
    suspend fun startLocalPortForward(
        localPort: Int,
        remoteHost: String,
        remotePort: Int,
    ) = withContext(Dispatchers.IO) {
        val ssh = client ?: error("Not connected")
        val params = net.schmizz.sshj.connection.channel.direct.Parameters(
            "127.0.0.1", localPort, remoteHost, remotePort,
        )
        val forwarder = ssh.newLocalPortForwarder(params, null)
        // forwarder.listen() блокирует поток — запускаем в отдельной корутине
        // TODO: обернуть в отдельный Dispatcher и хранить Job для отмены
        forwarder
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        runCatching { session?.close() }
        runCatching { client?.disconnect() }
        runCatching { client?.close() }
        session = null
        client = null
        _state.value = SshSessionState.DISCONNECTED
    }

    // ---- private ----

    private fun authenticate(ssh: SSHClient, username: String, auth: SshAuthMethod) {
        when (auth) {
            is SshAuthMethod.Password -> {
                ssh.authPassword(username, auth.password)
            }
            is SshAuthMethod.PublicKey -> {
                val kp: KeyProvider = ssh.loadKeys(
                    ByteArrayInputStream(auth.privateKeyPkcs8),
                    null,
                    object : PasswordFinder {
                        override fun reqPassword(resource: Resource<*>?): CharArray = CharArray(0)
                        override fun shouldRetry(resource: Resource<*>?): Boolean = false
                    },
                )
                ssh.authPublickey(username, kp)
            }
        }
    }

    private fun connectJumpHost(jump: app.termosh.core.ssh.model.ProxyJumpConfig): SSHClient {
        val jumpClient = SSHClient()
        jumpClient.addHostKeyVerifier(PromiscuousVerifier())
        jumpClient.connect(jump.host, jump.port)
        authenticate(
            jumpClient,
            jump.username,
            jump.auth,
        )
        return jumpClient
    }
}
