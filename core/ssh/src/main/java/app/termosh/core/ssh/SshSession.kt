package app.termosh.core.ssh

import app.termosh.core.ssh.model.SshAuthMethod
import app.termosh.core.ssh.model.SshConnectionConfig
import app.termosh.core.ssh.model.SshSessionState
import app.termosh.core.ssh.verifier.TofuHostKeyVerifier
import app.termosh.core.common.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.userauth.password.PasswordFinder
import net.schmizz.sshj.userauth.password.Resource
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean
import net.schmizz.sshj.connection.channel.direct.PTYMode

class SshSession(
    val id: String,
    val config: SshConnectionConfig,
    private val auth: SshAuthMethod,
    private val totpSecret: ByteArray?,
    private val totpGenerator: app.termosh.core.security.TotpGenerator?,
    private val tofu: TofuHostKeyVerifier?,
    private val scope: CoroutineScope,
) {
    private val writeMutex = Mutex()

    @Volatile
    private var corrupted = false

    var onCorrupted: ((Throwable) -> Unit)? = null

    private val _state = MutableStateFlow(SshSessionState.IDLE)
    val state: StateFlow<SshSessionState> = _state.asStateFlow()

    private val _output = MutableSharedFlow<String>(extraBufferCapacity = 1024)
    val output: SharedFlow<String> = _output.asSharedFlow()

    private val _disconnected = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val disconnected: SharedFlow<String> = _disconnected.asSharedFlow()

    private var jumpClient: SSHClient? = null
    private var client: SSHClient? = null
    private var session: Session? = null
    private var shell: Session.Shell? = null
    private var readerJob: Job? = null

    private val userClosed = AtomicBoolean(false)

    suspend fun connect(): Unit = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        userClosed.set(false)
        _state.value = SshSessionState.CONNECTING
        AppLogger.i("SSH connect start: ${config.username}@${config.host}:${config.port}")
        try {
            val ssh = openClient()
            AppLogger.i("SSH transport established, starting auth")
            _state.value = SshSessionState.AUTHENTICATING
            authenticate(ssh, config.username, auth, isTarget = true)
            AppLogger.i("SSH auth OK")
            ssh.connection.keepAlive.keepAliveInterval = config.keepAliveIntervalSec

            val sess = ssh.startSession()
            sess.allocatePTY("xterm-256color", 80, 24, 0, 0, mapOf(
                PTYMode.ECHO   to 1,
                PTYMode.ECHOE  to 1,
                PTYMode.ECHOK  to 1,
                PTYMode.ICANON to 1,
                PTYMode.ISIG   to 1,
                PTYMode.IEXTEN to 1,
                PTYMode.ICRNL  to 1,
                PTYMode.ONLCR  to 1,
                PTYMode.OPOST  to 1,
            ))
            session = sess
            client = ssh
            _state.value = SshSessionState.CONNECTED
            AppLogger.i("SSH session opened, ready for shell")
        } catch (t: Throwable) {
            AppLogger.e("SSH connect failed", t)
            _state.value = SshSessionState.ERROR
            hardClose()
            throw t
        }
    }
    fun startNewSession(): Session {
        val ssh = client ?: error("Not connected")
        return ssh.startSession()
    }

    suspend fun startShellOnExisting(): Session.Shell = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        shell?.let { return@withContext it }
        val sess = session ?: error("Not connected")
        val sh = sess.startShell()
        shell = sh
        AppLogger.i("SSH shell started")

        readerJob = scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val buf = ByteArray(4096)
            try {
                while (true) {
                    val n = sh.inputStream.read(buf)
                    if (n < 0) break
                    _output.emit(String(buf, 0, n, Charsets.UTF_8))
                }
            } catch (e: Throwable) {
                AppLogger.e("SSH reader stopped", e)
            } finally {
                _state.value = SshSessionState.DISCONNECTED
                if (!userClosed.get()) _disconnected.tryEmit("disconnected")
            }
        }
        sh
    }
    fun sshClient(): SSHClient? = client

    private fun hostKeyVerifier(): net.schmizz.sshj.transport.verification.HostKeyVerifier =
        tofu ?: PromiscuousVerifier()

    private fun openClient(): SSHClient {
        val jump = config.proxyJump
        if (jump == null) {
            val ssh = SSHClient()
            ssh.addHostKeyVerifier(hostKeyVerifier())
            ssh.connect(config.host, config.port)
            ssh.timeout = config.readTimeoutMs
            return ssh
        }

        val jc = SSHClient()
        jc.addHostKeyVerifier(hostKeyVerifier())
        jc.connect(jump.host, jump.port)
        jc.timeout = config.readTimeoutMs
        authenticate(jc, jump.username, jump.auth, isTarget = false)
        jumpClient = jc

        val dc = jc.newDirectConnection(config.host, config.port)
        val sock = DirectConnectionSocket(
            inputProvider = { dc.inputStream },
            outputProvider = { dc.outputStream },
            closeAction = { runCatching { dc.close() } },
        )

        val tc = SSHClient()
        tc.addHostKeyVerifier(hostKeyVerifier())
        tc.socketFactory = object : javax.net.SocketFactory() {
            override fun createSocket(): java.net.Socket = sock
            override fun createSocket(host: String?, port: Int): java.net.Socket = sock
            override fun createSocket(host: String?, port: Int, localHost: java.net.InetAddress?, localPort: Int): java.net.Socket = sock
            override fun createSocket(host: java.net.InetAddress?, port: Int): java.net.Socket = sock
            override fun createSocket(address: java.net.InetAddress?, port: Int, localAddress: java.net.InetAddress?, localPort: Int): java.net.Socket = sock
        }
        tc.connect(config.host, config.port)
        tc.timeout = config.readTimeoutMs
        // useJumpCredentialsForTarget — обрабатывается в authenticate(),
        // где при наличии флага подставляется auth jump-хоста
        return tc
    }


    /**
     * Выполнить команду на сервере в отдельном exec-канале и вернуть stdout+stderr.
     * Основной shell-канал не затрагивается.
     */
    suspend fun exec(command: String, timeoutSec: Int = 15): String = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val ssh = client ?: error("Not connected")
        val execSession = ssh.startSession()
        try {
            val cmd = execSession.exec(command)
            // Ждём завершения
            cmd.join(timeoutSec.toLong(), java.util.concurrent.TimeUnit.SECONDS)
            val stdout = cmd.inputStream.bufferedReader().readText()
            val stderr = cmd.errorStream.bufferedReader().readText()
            if (stdout.isNotBlank()) stdout else stderr
        } finally {
            runCatching { execSession.close() }
        }
    }

    fun write(data: ByteArray) {
        if (corrupted) return
        scope.launch(Dispatchers.IO) {
            writeMutex.withLock {
                val out = shell?.outputStream ?: return@withLock
                try {
                    out.write(data)
                    out.flush()
                } catch (t: Throwable) {
                    if (t is ArrayIndexOutOfBoundsException ||
                        t is android.os.NetworkOnMainThreadException) {
                        corrupted = true
                        onCorrupted?.invoke(t)
                    }
                }
            }
        }
    }

    suspend fun disconnect() {
        userClosed.set(true)
        hardClose()
        _state.value = SshSessionState.DISCONNECTED
    }

    private fun hardClose() {
        readerJob?.cancel()
        runCatching { shell?.close() }
        runCatching { session?.close() }
        runCatching { client?.disconnect() }
        runCatching { client?.close() }
        runCatching { jumpClient?.disconnect() }
        runCatching { jumpClient?.close() }
        shell = null
        session = null
        client = null
        jumpClient = null
    }

    private fun authenticate(
        ssh: SSHClient,
        username: String,
        a: SshAuthMethod,
        isTarget: Boolean = false,
    ) {
        val useJump = isTarget && config.useJumpCredentialsForTarget && config.proxyJump != null
        val effAuth: SshAuthMethod = if (useJump) config.proxyJump!!.auth else a
        val effUser: String = if (useJump) config.proxyJump!!.username else username

        when (effAuth) {
            is SshAuthMethod.Password -> {
                AppLogger.i("auth: password method, user=$effUser, pwdLen=${effAuth.password.size}")
                try {
                    ssh.authPassword(effUser, String(effAuth.password))
                    AppLogger.i("auth: password accepted")
                } catch (t: Throwable) {
                    AppLogger.e("auth: password rejected", t)
                    throw t
                }
            }
            is SshAuthMethod.PublicKeys -> {
                val passwordFinder = object : PasswordFinder {
                    override fun reqPassword(resource: Resource<*>?): CharArray? = null
                    override fun shouldRetry(resource: Resource<*>?): Boolean = false
                }
                var lastError: Throwable? = null
                var authenticated = false
                for (key in effAuth.keys) {
                    try {
                        val pem = toPem(key.privateKeyPkcs8)
                        val kp: KeyProvider = ssh.loadKeys(pem, null, passwordFinder)
                        ssh.authPublickey(effUser, kp)
                        authenticated = true
                        break
                    } catch (e: Throwable) {
                        lastError = e
                    }
                }
                if (!authenticated) {
                    throw lastError ?: IllegalStateException("All SSH keys rejected")
                }
            }
        }
    }

    private fun toPem(pkcs8: ByteArray): String {
        val b64 = Base64.getEncoder().encodeToString(pkcs8)
        val sb = StringBuilder()
        sb.append("-----BEGIN PRIVATE KEY-----\n")
        var i = 0
        while (i < b64.length) {
            val end = minOf(i + 64, b64.length)
            sb.append(b64, i, end).append('\n')
            i = end
        }
        sb.append("-----END PRIVATE KEY-----\n")
        return sb.toString()
    }


    fun write(text: String): Unit = write(text.toByteArray(Charsets.UTF_8))
}
