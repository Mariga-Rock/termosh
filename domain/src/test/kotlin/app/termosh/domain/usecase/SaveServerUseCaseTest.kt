package app.termosh.domain.usecase

import app.termosh.domain.model.DomainError
import app.termosh.domain.model.Server
import app.termosh.domain.model.ServerAuth
import app.termosh.domain.model.SecretRef
import app.termosh.domain.repository.ServerRepository
import app.termosh.domain.usecase.server.SaveServerUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveServerUseCaseTest {

    private val repo = mockk<ServerRepository>(relaxed = true)
    private val useCase = SaveServerUseCase(repo)

    private fun server(
        id: String = "",
        name: String = "prod",
        host: String = "example.com",
        port: Int = 22,
        username: String = "root",
    ) = Server(
        id = id,
        name = name,
        host = host,
        port = port,
        username = username,
        auth = ServerAuth.Password(SecretRef(byteArrayOf(1), byteArrayOf(2))),
        proxyJumpId = null,
        tags = emptyList(),
        createdAt = 0L,
        lastUsedAt = null,
    )

    @Test
    fun `assigns id when blank`() = runTest {
        coEvery { repo.save(any()) } returns Unit
        val r = useCase(server(id = ""))
        assertTrue(r.id.isNotBlank())
    }

    @Test
    fun `keeps id when set`() = runTest {
        coEvery { repo.save(any()) } returns Unit
        val r = useCase(server(id = "fixed"))
        assertEquals("fixed", r.id)
    }

    @Test
    fun `rejects blank name`() {
        assertThrows(DomainError.InvalidInput::class.java) {
            kotlinx.coroutines.runBlocking { useCase(server(name = "")) }
        }
    }

    @Test
    fun `rejects blank host`() {
        assertThrows(DomainError.InvalidInput::class.java) {
            kotlinx.coroutines.runBlocking { useCase(server(host = "")) }
        }
    }

    @Test
    fun `rejects blank username`() {
        assertThrows(DomainError.InvalidInput::class.java) {
            kotlinx.coroutines.runBlocking { useCase(server(username = "")) }
        }
    }

    @Test
    fun `rejects port out of range`() {
        assertThrows(DomainError.InvalidInput::class.java) {
            kotlinx.coroutines.runBlocking { useCase(server(port = 0)) }
        }
        assertThrows(DomainError.InvalidInput::class.java) {
            kotlinx.coroutines.runBlocking { useCase(server(port = 70000)) }
        }
    }

    @Test
    fun `accepts valid port 1`() = runTest {
        coEvery { repo.save(any()) } returns Unit
        useCase(server(port = 1))
        coVerify(exactly = 1) { repo.save(any()) }
    }

    @Test
    fun `accepts valid port 65535`() = runTest {
        coEvery { repo.save(any()) } returns Unit
        useCase(server(port = 65535))
        coVerify(exactly = 1) { repo.save(any()) }
    }
}
