package app.termosh.core.ssh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.connection.channel.direct.Session
import org.json.JSONArray

data class DockerContainer(
    val id: String,
    val shortId: String,
    val name: String,
    val image: String,
    val status: String,
    val state: String,
)

data class K8sPod(
    val name: String,
    val namespace: String,
    val status: String,
    val containers: List<String>,
)

class DockerManager {

    /**
     * Список контейнеров. Использует --format '{{json .}}' чтобы парсить построчно.
     */
    suspend fun listContainers(ssh: SshSession): List<DockerContainer> = withContext(Dispatchers.IO) {
        val cmd = "docker ps -a --no-trunc --format '{{json .}}'"
        val out = exec(ssh, cmd)
        val result = mutableListOf<DockerContainer>()
        out.lineSequence().forEach { line ->
            val t = line.trim()
            if (t.isEmpty() || !t.startsWith("{")) return@forEach
            runCatching {
                val o = org.json.JSONObject(t)
                val id = o.optString("ID", "")
                result += DockerContainer(
                    id = id,
                    shortId = id.take(12),
                    name = o.optString("Names", ""),
                    image = o.optString("Image", ""),
                    status = o.optString("Status", ""),
                    state = o.optString("State", ""),
                )
            }
        }
        result
    }

    suspend fun listPods(ssh: SshSession, namespace: String = ""): List<K8sPod> = withContext(Dispatchers.IO) {
        val nsFlag = if (namespace.isBlank()) "-A" else "-n $namespace"
        val cmd = "kubectl get pods $nsFlag -o json"
        val out = exec(ssh, cmd)
        val result = mutableListOf<K8sPod>()
        runCatching {
            val root = org.json.JSONObject(out)
            val items = root.optJSONArray("items") ?: JSONArray()
            for (i in 0 until items.length()) {
                val pod = items.getJSONObject(i)
                val meta = pod.optJSONObject("metadata") ?: continue
                val spec = pod.optJSONObject("spec") ?: continue
                val status = pod.optJSONObject("status") ?: continue

                val containers = mutableListOf<String>()
                spec.optJSONArray("containers")?.let { arr ->
                    for (j in 0 until arr.length()) containers += arr.getJSONObject(j).optString("name")
                }

                result += K8sPod(
                    name = meta.optString("name"),
                    namespace = meta.optString("namespace"),
                    status = status.optString("phase"),
                    containers = containers,
                )
            }
        }
        result
    }

    private suspend fun exec(ssh: SshSession, command: String): String {
        val session: Session = ssh.startNewSession()
        return try {
            val cmd = session.exec(command)
            cmd.join(15, java.util.concurrent.TimeUnit.SECONDS)
            val stdout = cmd.inputStream.bufferedReader().readText()
            val stderr = cmd.errorStream.bufferedReader().readText()
            if (stdout.isNotBlank()) stdout else stderr
        } finally {
            runCatching { session.close() }
        }
    }
}
