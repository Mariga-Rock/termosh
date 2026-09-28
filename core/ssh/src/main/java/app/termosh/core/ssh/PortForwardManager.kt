package app.termosh.core.ssh

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Parameters
import java.net.InetSocketAddress
import java.net.ServerSocket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortForwardManager @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = mutableMapOf<String, MutableList<Job>>()

    @Synchronized
    fun startAll(sessionId: String, ssh: SSHClient, specs: List<ForwardSpec>, onError: (Int, Throwable) -> Unit) {
        stopAll(sessionId)
        val list = mutableListOf<Job>()
        specs.forEach { spec ->
            val job = scope.launch {
                try {
                    val serverSocket = ServerSocket()
                    serverSocket.reuseAddress = true
                    serverSocket.bind(InetSocketAddress("127.0.0.1", spec.localPort))
                    val params = Parameters("127.0.0.1", spec.localPort, spec.remoteHost, spec.remotePort)
                    val forwarder = ssh.newLocalPortForwarder(params, serverSocket)
                    forwarder.listen()
                } catch (t: Throwable) {
                    onError(spec.localPort, t)
                }
            }
            list += job
        }
        jobs[sessionId] = list
    }

    @Synchronized
    fun stopAll(sessionId: String) {
        jobs.remove(sessionId)?.forEach { it.cancel() }
    }

    @Synchronized
    fun stopAll() {
        jobs.values.flatten().forEach { it.cancel() }
        jobs.clear()
    }
}
