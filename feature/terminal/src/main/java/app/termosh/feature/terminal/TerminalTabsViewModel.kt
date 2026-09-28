package app.termosh.feature.terminal

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.termosh.core.common.AppLogger
import app.termosh.core.datastore.SettingsDataStore
import app.termosh.core.mosh.MoshManager
import app.termosh.core.security.codec.CryptoSecretCodec
import app.termosh.core.service.SessionControl
import app.termosh.core.service.TermoshSessionService
import app.termosh.core.ssh.ForwardSpec
import app.termosh.core.ssh.PortForwardManager
import app.termosh.core.ssh.SshSessionManager
import app.termosh.core.ssh.model.SshSessionState
import app.termosh.core.terminal.SessionLogger
import app.termosh.core.terminal.TerminalBufferRegistry
import app.termosh.domain.model.Server
import app.termosh.domain.repository.ServerRepository
import app.termosh.domain.repository.TotpRepository
import app.termosh.domain.usecase.portforward.GetEnabledPortForwardsUseCase
import app.termosh.domain.usecase.server.GetServerUseCase
import app.termosh.domain.usecase.server.ObserveServersUseCase
import app.termosh.domain.usecase.snippet.ObserveSnippetsUseCase
import app.termosh.feature.terminal.connect.ReconnectScheduler
import app.termosh.feature.terminal.connect.SshConfigBuilder
import app.termosh.feature.terminal.input.InputComposer
import app.termosh.feature.terminal.mosh.MoshController
import app.termosh.feature.terminal.startup.StartupScriptRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import app.termosh.core.mosh.MoshProcess

@HiltViewModel
class TerminalTabsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val getServer: GetServerUseCase,
    observeServers: ObserveServersUseCase,
    observeSnippets: ObserveSnippetsUseCase,
    private val codec: CryptoSecretCodec,
    private val serverRepository: ServerRepository,
    private val totpRepository: TotpRepository,
    private val licenseRepository: app.termosh.core.licensing.LicenseRepository,
    private val sessions: SshSessionManager,
    private val buffers: TerminalBufferRegistry,
    private val settings: SettingsDataStore,
    private val logger: SessionLogger,
    private val getEnabledForwards: GetEnabledPortForwardsUseCase,
    private val portForwards: PortForwardManager,
    private val moshManager: MoshManager,
    private val commandNotifier: app.termosh.core.service.CommandNotifier,
    private val configBuilder: SshConfigBuilder,
    private val startupRunner: StartupScriptRunner,
    private val moshController: MoshController,
    private val reconnectScheduler: ReconnectScheduler,
) : ViewModel() {

    private val startServerId: String = savedState.get<String>("serverId") ?: ""

    private val _state = MutableStateFlow(TabsUiState())
    val state: StateFlow<TabsUiState> = _state.asStateFlow()

    private val watchers = mutableMapOf<String, Job>()
    private val logFiles = mutableMapOf<String, File>()

    @Volatile private var logEnabled = false
    @Volatile private var notifyOnFinish = false

    init {
        viewModelScope.launch { settings.logSessions.collect { logEnabled = it } }
        viewModelScope.launch { settings.notifyOnFinish.collect { notifyOnFinish = it } }
        viewModelScope.launch {
            while (true) {
                delay(1500)
            }
        }
        viewModelScope.launch {
            observeServers().collect { list ->
                AppLogger.i("observeServers: список = ${list.size} серверов")
                _state.value = _state.value.copy(availableServers = list)
            }
        }
        viewModelScope.launch {
            observeSnippets().collect { list ->
                _state.value = _state.value.copy(snippets = list)
            }
        }
        viewModelScope.launch {
            SessionControl.disconnectAll.collect { disconnectAllTabs() }
        }
        viewModelScope.launch {
            licenseRepository.status.collect { st ->
                _state.value = _state.value.copy(
                    isPro = st is app.termosh.core.licensing.model.LicenseStatus.Activated,
                )
            }
        }
        if (startServerId.isNotBlank()) openTabForServer(startServerId)
    }

    fun bufferFor(tabId: String) = buffers.get(tabId)
    fun openPicker() {
        AppLogger.i("openPicker: availableServers=${_state.value.availableServers.size}, tabs=${_state.value.tabs.size}")
        _state.value = _state.value.copy(showPicker = true)
    }
    fun dismissPicker() { _state.value = _state.value.copy(showPicker = false) }
    fun dismissTabLimitDialog() { _state.value = _state.value.copy(showTabLimitDialog = false) }
    fun openSnippets() { _state.value = _state.value.copy(showSnippets = true) }
    fun dismissSnippets() { _state.value = _state.value.copy(showSnippets = false) }

    fun sendSnippet(command: String) {
        sendText(command)
        _state.value = _state.value.copy(showSnippets = false)
    }

    fun openTabForServer(serverId: String) {
        val s = _state.value
        if (!s.isPro && s.tabs.size >= s.freeTabLimit) {
            _state.value = s.copy(showTabLimitDialog = true, showPicker = false)
            return
        }
        _state.value = _state.value.copy(showPicker = false)
        val tabId = UUID.randomUUID().toString()
        viewModelScope.launch {
            try {
                AppLogger.i("loading server config")
                val server = getServer(serverId)
                AppLogger.i("server: ${server.name} ${server.username}@${server.host}:${server.port} useMosh=${server.useMosh}")
                val auth = configBuilder.authOf(server)
                AppLogger.i("auth resolved: ${auth::class.simpleName}")
                val config = configBuilder.buildConfig(server)
                AppLogger.i("config: host=${config.host} port=${config.port} proxyJump=${config.proxyJump != null}")
                val totpSecret = configBuilder.resolveTotpSecret(server, totpRepository)
                val session = sessions.open(tabId, config, auth, totpSecret)

                _state.value = _state.value.copy(
        tabStatuses = _state.value.tabStatuses + (tabId to TabStatus(TerminalConnectionState.CONNECTING, "Connecting to ${server.name}...")),
                    tabs = _state.value.tabs + TerminalTab(tabId, serverId, server.name, server.useMosh),
                    activeTabId = tabId,
                    connectionState = TerminalConnectionState.CONNECTING,
                    statusMessage = "Connecting to ${server.name}...",
                )

                if (logEnabled) {
                    val f = logger.createLogFile(server.name)
                    logFiles[tabId] = f
                    logger.append(f, "=== Session ${server.name} ${server.username}@${server.host}:${server.port} ===\n")
                }

                buffers.get(tabId).clear()
                buffers.get(tabId).write("Connecting to ${server.username}@${server.host}:${server.port}...\n")

                session.connect()
                runCatching { serverRepository.markUsed(serverId, System.currentTimeMillis()) }

                if (server.useMosh) {
                    buffers.get(tabId).write("Starting mosh-server on remote...\n")
                    try {
                        val mosh = moshManager.start(
                            host = server.host,
                            sshSession = session,
                            columns = 80,
                            lines = 24,
                            sshPort = server.port,
                        )
                        moshController.register(tabId, mosh)
                        buffers.get(tabId).write("mosh connected (UDP ${mosh.udpPort}).\n\n")

                        _state.value = _state.value.copy(
        tabStatuses = _state.value.tabStatuses + (tabId to TabStatus(TerminalConnectionState.CONNECTED, "mosh connected to ${server.name}")),
                            connectionState = TerminalConnectionState.CONNECTED,
                            statusMessage = "mosh connected to ${server.name}",
                            retryAttempt = 0,
                        )
                        startForeground(server.name)
                        startMoshReader(tabId, mosh.process.inputStream)
                        startMoshStateWatcher(tabId, mosh)
                        runStartupScript(server, tabId)
                    } catch (t: Throwable) {
                        buffers.get(tabId).write("mosh ERROR: ${t.message}\n")
                        buffers.get(tabId).write("Откат на обычный SSH...\n\n")
                        session.startShellOnExisting()
                        _state.value = _state.value.copy(
        tabStatuses = _state.value.tabStatuses + (tabId to TabStatus(TerminalConnectionState.CONNECTED, "SSH (mosh failed) — ${server.name}")),
                            connectionState = TerminalConnectionState.CONNECTED,
                            statusMessage = "SSH (mosh failed) — ${server.name}",
                            retryAttempt = 0,
                        )
                        startForeground(server.name)
                        watchSsh(tabId, serverId, server.name)
                    }
                } else {
                    session.startShellOnExisting()
                    buffers.get(tabId).write("Connected.\n\n")
                    _state.value = _state.value.copy(
        tabStatuses = _state.value.tabStatuses + (tabId to TabStatus(TerminalConnectionState.CONNECTED, "Connected to ${server.name}")),
                        connectionState = TerminalConnectionState.CONNECTED,
                        statusMessage = "Connected to ${server.name}",
                        retryAttempt = 0,
                    )
                    startForeground(server.name)

                    runCatching {
                        val fwds = getEnabledForwards(serverId)
                        if (fwds.isNotEmpty()) {
                            val ssh = session.sshClient()
                            if (ssh != null) {
                                portForwards.startAll(
                                    sessionId = tabId,
                                    ssh = ssh,
                                    specs = fwds.map { ForwardSpec(it.localPort, it.remoteHost, it.remotePort) },
                                    onError = { port, t ->
                                        buffers.get(tabId).write("\n[forward 127.0.0.1:$port failed: ${t.message ?: "?"}]\n")
                                    },
                                )
                                buffers.get(tabId).write("[port forwards: ${fwds.size}]\n")
                            }
                        }
                    }
                    watchSsh(tabId, serverId, server.name)
                    runStartupScript(server, tabId)
                }
            } catch (t: Throwable) {
                val full = buildString {
                    append("ERROR: ").append(t.javaClass.name)
                        .append(": ").append(t.message ?: "(no message)").append("\n")
                    val sw = java.io.StringWriter()
                    t.printStackTrace(java.io.PrintWriter(sw))
                    append(sw.toString()).append("\n")
                }
                buffers.get(tabId).write(full)
                _state.value = _state.value.copy(
        tabStatuses = _state.value.tabStatuses + (tabId to TabStatus(TerminalConnectionState.ERROR, t.message ?: t.javaClass.simpleName)),
                    connectionState = TerminalConnectionState.ERROR,
                    statusMessage = t.message ?: t.javaClass.simpleName,
                )
            }
        }
    }

    // ============ Mosh reader ============
    private fun startMoshReader(tabId: String, input: InputStream) {
        watchers[tabId]?.cancel()
        watchers[tabId] = viewModelScope.launch(Dispatchers.IO) {
            val buf = ByteArray(4096)
            try {
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    val text = String(buf, 0, n, Charsets.UTF_8)
                    buffers.get(tabId).write(text)
logFiles[tabId]?.let { logger.append(it, text) }
                }
            } catch (_: Throwable) {
            } finally {
                _state.value = _state.value.copy(
        tabStatuses = _state.value.tabStatuses + (tabId to TabStatus(TerminalConnectionState.DISCONNECTED, "mosh disconnected")),
                    connectionState = TerminalConnectionState.DISCONNECTED,
                    statusMessage = "mosh disconnected",
                )
                buffers.get(tabId).write("\n[mosh disconnected]\n")
            }
        }
    }

    private fun startMoshStateWatcher(tabId: String, process: MoshProcess) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { process.pty.waitFor() }
            val code = runCatching { process.pty.waitFor() }.getOrDefault(-1)
            buffers.get(tabId).write("\n[mosh-client exited: $code]\n")
            _state.value = _state.value.copy(
        tabStatuses = _state.value.tabStatuses + (tabId to TabStatus(TerminalConnectionState.DISCONNECTED, "mosh-client exited ($code)")),
                connectionState = TerminalConnectionState.DISCONNECTED,
                statusMessage = "mosh-client exited ($code)",
            )
        }
    }

    // ============ SSH reader ============
    private fun watchSsh(tabId: String, serverId: String, name: String) {
        val session = sessions.get(tabId) ?: return
        watchers[tabId]?.cancel()
        watchers[tabId] = viewModelScope.launch {
            launch {
                session.output.collect { text ->
                    buffers.get(tabId).write(text)
logFiles[tabId]?.let { logger.append(it, text) }
                }
            }
            launch {
                session.disconnected.collect {
                    if (moshController.has(tabId)) return@collect
                    if (_state.value.activeTabId == tabId) {
                        _state.value = _state.value.copy(
        tabStatuses = _state.value.tabStatuses + (tabId to TabStatus(TerminalConnectionState.RECONNECTING, "Connection lost, reconnecting...")),
                            connectionState = TerminalConnectionState.RECONNECTING,
                            statusMessage = "Connection lost, reconnecting...",
                        )
                    }
                    scheduleRetry(tabId, serverId, name)
                }
            }
        }
    }

    private fun scheduleRetry(tabId: String, serverId: String, name: String) {
        reconnectScheduler.schedule(
            scope = viewModelScope,
            tabId = tabId,
            maxAttempts = _state.value.retryMax,
            onAttempt = { attempt, waitMs ->
                if (_state.value.activeTabId == tabId) {
                    _state.value = _state.value.copy(
                        retryAttempt = attempt,
                        statusMessage = "Reconnect ($attempt/${_state.value.retryMax}) через ${waitMs / 1000}с...",
                    )
                }
                buffers.get(tabId).write("\n[reconnect $attempt/${_state.value.retryMax} через ${waitMs / 1000}с]\n")
            },
            onReconnect = {
                val server = getServer(serverId)
                sessions.close(tabId)
                val config = configBuilder.buildConfig(server)
                val totpSecret = configBuilder.resolveTotpSecret(server, totpRepository)
                val sess = sessions.open(tabId, config, configBuilder.authOf(server), totpSecret)
                sess.connect()
                buffers.get(tabId).write("Reconnected.\n\n")
                if (_state.value.activeTabId == tabId) {
                    _state.value = _state.value.copy(
        tabStatuses = _state.value.tabStatuses + (tabId to TabStatus(TerminalConnectionState.CONNECTED, "Connected to $name")),
                        connectionState = TerminalConnectionState.CONNECTED,
                        statusMessage = "Connected to $name",
                        retryAttempt = 0,
                    )
                }
                watchSsh(tabId, serverId, name)
                true
            },
            onFailed = {
                if (_state.value.activeTabId == tabId) {
                    _state.value = _state.value.copy(
        tabStatuses = _state.value.tabStatuses + (tabId to TabStatus(TerminalConnectionState.ERROR, "Не удалось переподключиться")),
                        connectionState = TerminalConnectionState.ERROR,
                        statusMessage = "Не удалось переподключиться",
                    )
                }
            },
        )
    }

    @Suppress("unused")
    private fun mapState(s: SshSessionState) = when (s) {
        SshSessionState.CONNECTED -> TerminalConnectionState.CONNECTED
        SshSessionState.CONNECTING, SshSessionState.AUTHENTICATING -> TerminalConnectionState.CONNECTING
        SshSessionState.ERROR -> TerminalConnectionState.ERROR
        else -> TerminalConnectionState.DISCONNECTED
    }

    fun selectTab(tabId: String) { _state.value = _state.value.copy(activeTabId = tabId) }

    fun reconnectActive() {
        val id = _state.value.activeTabId ?: return
        val tab = _state.value.tabs.firstOrNull { it.tabId == id } ?: return
        closeTab(id)
        openTabForServer(tab.serverId)
    }

    fun toggleSplit() {
        val s = _state.value
        val cur = s.activeTabId ?: return
        if (s.splitTabId != null) {
            _state.value = s.copy(splitTabId = null)
            return
        }
        val other = s.tabs.firstOrNull { it.tabId != cur } ?: return
        _state.value = s.copy(splitTabId = other.tabId, splitActivePane = 0)
    }

    fun setSplitActivePane(pane: Int) {
        if (_state.value.splitTabId == null) return
        _state.value = _state.value.copy(splitActivePane = pane)
    }

    fun setSplitOrientation(vertical: Boolean) {
        _state.value = _state.value.copy(splitOrientationVertical = vertical)
    }

    fun swapSplit() {
        val s = _state.value
        val a = s.activeTabId ?: return
        val b = s.splitTabId ?: return
        _state.value = s.copy(activeTabId = b, splitTabId = a, splitActivePane = 0)
    }

    fun closeTab(tabId: String) {
        val tab = _state.value.tabs.firstOrNull { it.tabId == tabId }
        watchers.remove(tabId)?.cancel()
        reconnectScheduler.cancel(tabId)
        portForwards.stopAll(tabId)
        moshController.stop(tabId)
        sessions.close(tabId)
        buffers.remove(tabId)
        logFiles.remove(tabId)
_state.value = _state.value.copy(tabStatuses = _state.value.tabStatuses - tabId)
        val newTabs = _state.value.tabs.filterNot { it.tabId == tabId }
        val newActive = when {
            _state.value.activeTabId != tabId -> _state.value.activeTabId
            newTabs.isEmpty() -> null
            else -> newTabs.last().tabId
        }
        val newSplit = if (_state.value.splitTabId == tabId) null else _state.value.splitTabId
        _state.value = _state.value.copy(tabs = newTabs, activeTabId = newActive, splitTabId = newSplit)
        if (newTabs.isEmpty()) stopForeground()
    }

    fun setInput(v: String) { _state.value = _state.value.copy(input = v) }

    fun toggleModifier(name: String) {
        val cur = _state.value.modifiers
        val next = if (name in cur) cur - name else cur + name
        _state.value = _state.value.copy(modifiers = next)
    }

    private fun activeSendTabId(): String? {
        val s = _state.value
        return if (s.splitTabId != null && s.splitActivePane == 1) s.splitTabId else s.activeTabId
    }

    fun sendText(text: String) {
        val id = activeSendTabId()
        if (id == null) {
            app.termosh.core.common.AppLogger.w("sendText: activeSendTabId() == null, text dropped: '$text'")
            return
        }
        val bytes = text.toByteArray(Charsets.UTF_8)
        val hasMosh = moshController.has(id)
        val session = sessions.get(id)
        app.termosh.core.common.AppLogger.i("sendText: id=$id, len=${bytes.size}, hasMosh=$hasMosh, session=$session")
        if (hasMosh) {
            moshController.write(id, bytes)
        } else if (session != null) {
            session.write(bytes)
        } else {
            app.termosh.core.common.AppLogger.w("sendText: no mosh and no session for id=$id")
        }
    }

    fun sendBackspace() = sendText("\u007F")
    fun sendEnter() = sendText("\r")
    fun sendSpace() = sendText(" ")

    fun sendRawKey(seq: String) = sendText(seq)

    fun submitInput() {
        val id = activeSendTabId() ?: return
        val payload = InputComposer.compose(_state.value.input, _state.value.modifiers)
        if (moshController.has(id)) {
            moshController.write(id, payload)
        } else {
            sessions.get(id)?.write(payload)
        }
_state.value = _state.value.copy(input = "", modifiers = emptySet())
    }

    fun cancelReconnect() {
        _state.value.activeTabId?.let { id ->
            reconnectScheduler.cancel(id)
            _state.value = _state.value.copy(
        tabStatuses = _state.value.tabStatuses + (id to TabStatus(TerminalConnectionState.DISCONNECTED, "Reconnect отменён")),
                connectionState = TerminalConnectionState.DISCONNECTED,
                statusMessage = "Reconnect отменён",
            )
        }
    }

    fun disconnectActive() {
        val id = activeSendTabId() ?: return
        reconnectScheduler.cancel(id)
        watchers.remove(id)?.cancel()
        portForwards.stopAll(id)
        moshController.stop(id)
        viewModelScope.launch { sessions.get(id)?.disconnect() }
        _state.value = _state.value.copy(
        tabStatuses = _state.value.tabStatuses + (id to TabStatus(TerminalConnectionState.DISCONNECTED, "Disconnected")),
            connectionState = TerminalConnectionState.DISCONNECTED,
            statusMessage = "Disconnected",
        )
    }

    fun disconnectAllTabs() {
        sessions.ids().forEach { id ->
            reconnectScheduler.cancel(id)
            watchers.remove(id)?.cancel()
            portForwards.stopAll(id)
            moshController.stop(id)
            viewModelScope.launch { sessions.get(id)?.disconnect() }
        }
        _state.value = _state.value.copy(
        tabStatuses = _state.value.tabs.associate { it.tabId to TabStatus(TerminalConnectionState.DISCONNECTED, "Disconnected") },
            connectionState = TerminalConnectionState.DISCONNECTED,
            statusMessage = "Disconnected",
        )
    }

    private fun startForeground(name: String) {
        runCatching { TermoshSessionService.start(appContext, "Termosh: $name") }
    }
    private fun stopForeground() {
        runCatching { TermoshSessionService.stop(appContext) }
    }

    private suspend fun runStartupScript(server: Server, tabId: String) {
        val steps = startupRunner.buildSteps(server)
        steps.forEach { step ->
            writeToTab(tabId, step.text)
            delay(step.delayMs)
        }
    }

    private fun writeToTab(tabId: String, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        if (!moshController.write(tabId, bytes)) {
            sessions.get(tabId)?.write(bytes)
        }
    }

    fun sendBracketedPaste(text: String) {
        if (text.isEmpty()) return
        val id = activeSendTabId() ?: return
        val payload = "\u001b[200~" + text + "\u001b[201~"
        val bytes = payload.toByteArray(Charsets.UTF_8)
        if (!moshController.write(id, bytes)) {
            sessions.get(id)?.write(bytes)
        }
    }

    fun getAllOutput(): String {
        val id = _state.value.activeTabId ?: return ""
        return buffers.get(id).snapshot()
    }

    fun clearActiveBuffer() {
        val id = _state.value.activeTabId ?: return
        buffers.get(id).clear()
    }

}
