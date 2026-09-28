package app.termosh.feature.settings.backup

import app.termosh.domain.model.Server
import app.termosh.domain.model.ServerAuth
import app.termosh.domain.model.SecretRef
import app.termosh.domain.model.SshKey
import app.termosh.domain.model.SshKeyType
import app.termosh.domain.repository.KnownHostRepository
import app.termosh.domain.repository.PortForwardRepository
import app.termosh.domain.repository.SecretCodec
import app.termosh.domain.repository.ServerRepository
import app.termosh.domain.repository.SnippetRepository
import app.termosh.domain.repository.SshKeyRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {

    private val servers = mockk<ServerRepository>()
    private val keys = mockk<SshKeyRepository>()
    private val snippets = mockk<SnippetRepository>()
    private val knownHosts = mockk<KnownHostRepository>(relaxed = true)
    private val portForwards = mockk<PortForwardRepository>(relaxed = true)
    private val codec = mockk<SecretCodec>()

    private val manager = BackupManager(
        servers, keys, snippets, knownHosts, portForwards, codec,
    )

    private fun sampleServer() = Server(
        id = "s1", name = "prod", host = "1.2.3.4", port = 22, username = "root",
        auth = ServerAuth.Password(SecretRef(byteArrayOf(1), byteArrayOf(2))),
        proxyJumpId = null, tags = listOf("work"),
        createdAt = 1L, lastUsedAt = null,
    )

    private fun sampleKey() = SshKey(
        id = "k1", type = SshKeyType.ED25519,
        privateKeyRef = SecretRef(byteArrayOf(3), byteArrayOf(4)),
        publicKeyOpenSsh = "ssh-ed25519 AAAA",
        fingerprintSha256 = "SHA256:aaa",
        comment = null, createdAt = 2L,
    )

    @Test
    fun `export config does not include secrets`() = runTest {
        coEvery { servers.observeAll() } returns flowOf(listOf(sampleServer()))
        coEvery { keys.observeAll() } returns flowOf(listOf(sampleKey()))
        coEvery { snippets.observeAll() } returns flowOf(emptyList())
        every { codec.decryptString(any()) } returns "SECRET"

        val data = manager.export(includeSecrets = false)
        assertTrue(!data.includesSecrets)
        assertEquals("termosh", data.format)
        assertNull(data.servers[0].passwordPlain)
        assertNull(data.keys[0].privateKeyPkcs8Base64)
    }

    @Test
    fun `export vault includes secrets`() = runTest {
        coEvery { servers.observeAll() } returns flowOf(listOf(sampleServer()))
        coEvery { keys.observeAll() } returns flowOf(listOf(sampleKey()))
        coEvery { snippets.observeAll() } returns flowOf(emptyList())
        every { codec.decryptString(any()) } returns "SECRET"
        every { codec.decrypt(any()) } returns byteArrayOf(9, 9, 9)

        val data = manager.export(includeSecrets = true)
        assertTrue(data.includesSecrets)
        assertEquals("termoshvault", data.format)
        assertEquals("SECRET", data.servers[0].passwordPlain)
        assertTrue(data.keys[0].privateKeyPkcs8Base64 != null)
    }

    @Test
    fun `import without secrets creates empty password`() = runTest {
        every { codec.encryptString(any()) } returns SecretRef(byteArrayOf(9), byteArrayOf(9))
        coEvery { servers.save(any()) } returns Unit
        coEvery { keys.save(any()) } returns Unit
        coEvery { snippets.save(any()) } returns Unit

        val data = BackupData(
            format = "termosh",
            formatVersion = 1,
            appVersion = "0.1.0",
            exportedAt = 0L,
            includesSecrets = false,
            servers = listOf(
                BackupServer(
                    id = "s1", name = "p", host = "h", port = 22, username = "u",
                    tags = emptyList(), authType = "PASSWORD",
                    passwordPlain = null, keyIds = null, proxyJumpId = null,
                    useMosh = false, totpSecretId = null,
                    startupCommands = emptyList(), envVars = emptyMap(),
                    envSecretsPlain = emptyMap(),
                ),
            ),
            keys = emptyList(), snippets = emptyList(),
            knownHosts = emptyList(), portForwards = emptyList(),
        )
        val stats = manager.import(data, secretsAvailable = false)
        assertEquals(1, stats.serversImported)
        assertEquals(1, stats.serversWithoutAuth)
        assertTrue(!stats.includesSecrets)
    }
}
