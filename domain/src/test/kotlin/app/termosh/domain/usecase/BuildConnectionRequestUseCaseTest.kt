package app.termosh.domain.usecase

import app.termosh.domain.model.ConnectionAuth
import app.termosh.domain.model.Server
import app.termosh.domain.model.ServerAuth
import app.termosh.domain.model.SecretRef
import app.termosh.domain.repository.SecretCodec
import app.termosh.domain.repository.ServerRepository
import app.termosh.domain.repository.SshKeyRepository
import app.termosh.domain.usecase.connection.BuildConnectionRequestUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildConnectionRequestUseCaseTest {

    private val servers = mockk<ServerRepository>()
    private val keys = mockk<SshKeyRepository>(relaxed = true)
    private val codec = mockk<SecretCodec>()

    private val useCase = BuildConnectionRequestUseCase(servers, keys, codec)

    @Test
    fun `resolves password auth`() = runTest {
        val ref = SecretRef(byteArrayOf(1), byteArrayOf(2))
        coEvery { servers.getById("s1") } returns Server(
            id = "s1", name = "n", host = "h", port = 22, username = "u",
            auth = ServerAuth.Password(ref), proxyJumpId = null,
            tags = emptyList(), createdAt = 0L, lastUsedAt = null,
        )
        every { codec.decryptString(ref) } returns "secret"

        val req = useCase("s1")
        assertTrue(req.auth is ConnectionAuth.Password)
    }
}
