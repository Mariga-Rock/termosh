package app.termosh.domain.usecase.portforward

import app.termosh.domain.model.DomainError
import app.termosh.domain.model.PortForward
import app.termosh.domain.repository.PortForwardRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SavePortForwardUseCaseTest {

    private val repo = mockk<PortForwardRepository>(relaxed = true)
    private val useCase = SavePortForwardUseCase(repo)

    private fun makePF(
        localPort: Int = 8080,
        remoteHost: String = "127.0.0.1",
        remotePort: Int = 80,
    ) = PortForward(
        id = "", serverId = "s1", localPort = localPort,
        remoteHost = remoteHost, remotePort = remotePort,
        enabled = true, createdAt = 0L,
    )

    @Test
    fun `assigns id when blank`() = runTest {
        coEvery { repo.save(any()) } returns Unit
        val r = useCase(makePF())
        assertTrue(r.id.isNotBlank())
        coVerify(exactly = 1) { repo.save(any()) }
    }

    @Test
    fun `keeps id when set`() = runTest {
        coEvery { repo.save(any()) } returns Unit
        val r = useCase(makePF().copy(id = "fixed"))
        assertEquals("fixed", r.id)
    }

    @Test
    fun `rejects local port out of range`() {
        assertThrows(DomainError.InvalidInput::class.java) {
            kotlinx.coroutines.runBlocking { useCase(makePF(localPort = 0)) }
        }
        assertThrows(DomainError.InvalidInput::class.java) {
            kotlinx.coroutines.runBlocking { useCase(makePF(localPort = 70000)) }
        }
    }

    @Test
    fun `rejects remote port out of range`() {
        assertThrows(DomainError.InvalidInput::class.java) {
            kotlinx.coroutines.runBlocking { useCase(makePF(remotePort = -1)) }
        }
    }

    @Test
    fun `rejects blank remote host`() {
        assertThrows(DomainError.InvalidInput::class.java) {
            kotlinx.coroutines.runBlocking { useCase(makePF(remoteHost = "")) }
        }
    }

    @Test
    fun `accepts valid config`() = runTest {
        coEvery { repo.save(any()) } returns Unit
        val r = useCase(makePF(localPort = 1, remotePort = 65535))
        assertTrue(r.id.isNotBlank())
    }
}
