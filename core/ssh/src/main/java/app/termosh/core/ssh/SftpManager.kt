package app.termosh.core.ssh

import app.termosh.core.ssh.model.SshAuthMethod
import app.termosh.core.ssh.model.SshConnectionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.userauth.password.PasswordFinder
import net.schmizz.sshj.userauth.password.Resource
import java.io.InputStream
import java.io.OutputStream
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

data class RemoteFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val mtimeSec: Long,
)

@Singleton
class SftpManager @Inject constructor() {

    @Volatile private var ssh: SSHClient? = null
    @Volatile private var sftp: SFTPClient? = null

    suspend fun connect(
        config: SshConnectionConfig,
        auth: SshAuthMethod,
    ) = withContext(Dispatchers.IO) {
        disconnect()
        val client = SSHClient()
        client.addHostKeyVerifier(PromiscuousVerifier())
        client.connect(config.host, config.port)
        client.timeout = config.readTimeoutMs

        when (auth) {
            is SshAuthMethod.Password -> client.authPassword(config.username, String(auth.password))
            is SshAuthMethod.PublicKeys -> {
                val pf = object : PasswordFinder {
                    override fun reqPassword(resource: Resource<*>?): CharArray? = null
                    override fun shouldRetry(resource: Resource<*>?): Boolean = false
                }
                var ok = false
                var lastErr: Throwable? = null
                for (k in auth.keys) {
                    try {
                        val kp: KeyProvider = client.loadKeys(toPem(k.privateKeyPkcs8), null, pf)
                        client.authPublickey(config.username, kp)
                        ok = true
                        break
                    } catch (t: Throwable) {
                        lastErr = t
                    }
                }
                if (!ok) throw lastErr ?: IllegalStateException("All keys rejected")
            }
        }

        ssh = client
        sftp = client.newSFTPClient()
    }

    suspend fun list(path: String): List<RemoteFile> = withContext(Dispatchers.IO) {
        val c = sftp ?: error("SFTP not connected")
        c.ls(path).map { it.toRemote() }.sortedWith(
            compareByDescending<RemoteFile> { it.isDirectory }.thenBy { it.name.lowercase() }
        )
    }

    suspend fun download(remote: String, out: OutputStream) = withContext(Dispatchers.IO) {
        val c = sftp ?: error("SFTP not connected")
        c.open(remote, java.util.EnumSet.of(net.schmizz.sshj.sftp.OpenMode.READ)).use { file ->
            val ins = file.RemoteFileInputStream(0)
            ins.copyTo(out)
        }
        out.flush()
    }

    suspend fun upload(input: InputStream, remote: String) = withContext(Dispatchers.IO) {
        val c = sftp ?: error("SFTP not connected")
        c.open(
            remote,
            java.util.EnumSet.of(
                net.schmizz.sshj.sftp.OpenMode.WRITE,
                net.schmizz.sshj.sftp.OpenMode.CREAT,
                net.schmizz.sshj.sftp.OpenMode.TRUNC,
            ),
        ).use { file ->
            val outs = file.RemoteFileOutputStream(0)
            input.copyTo(outs)
            outs.close()
        }
    }

    suspend fun deleteFile(path: String) = withContext(Dispatchers.IO) {
        sftp?.rm(path) ?: error("SFTP not connected")
    }

    suspend fun deleteDir(path: String) = withContext(Dispatchers.IO) {
        sftp?.rmdir(path) ?: error("SFTP not connected")
    }

    suspend fun mkdir(path: String) = withContext(Dispatchers.IO) {
        sftp?.mkdir(path) ?: error("SFTP not connected")
    }

    suspend fun rename(from: String, to: String) = withContext(Dispatchers.IO) {
        sftp?.rename(from, to) ?: error("SFTP not connected")
    }

    fun disconnect() {
        runCatching { sftp?.close() }
        runCatching { ssh?.disconnect() }
        runCatching { ssh?.close() }
        sftp = null
        ssh = null
    }

    private fun RemoteResourceInfo.toRemote(): RemoteFile {
        val isDir = attributes.type == FileMode.Type.DIRECTORY
        return RemoteFile(
            name = name,
            path = path,
            isDirectory = isDir,
            size = attributes.size,
            mtimeSec = attributes.mtime,
        )
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
}
