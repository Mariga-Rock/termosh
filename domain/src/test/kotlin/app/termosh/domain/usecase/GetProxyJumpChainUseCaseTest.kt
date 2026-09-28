package app.termosh.domain.usecase

import app.termosh.domain.model.DomainError
import app.termosh.domain.model.Server
import app.termosh.domain.model.ServerAuth
import app.termosh.domain.model.SecretRef
import app.termosh.domain.repository.ServerRepository
import app.termosh.domain.usecase.connection.GetProxyJumpChainUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GetProxyJumpChainUseCaseTest {

    private val repo = mockk<ServerRepository>()
    private val useCase = GetProxyJumpChainUseCase(repo)

    private fun server(id: String, jumpId: String? = null) = Server(
        id = id,
        name = "srv-$id",
        host = "$id.example.com",
        port = 22,
        username = "root",
        auth = ServerAuth.Password(SecretRef(byteArrayOf(1), byteArrayOf(2))),
        proxyJumpId = jumpId,
        tags = emptyList(),
        createdAt = 0L,
        lastUsedAt = null,
    )

    @Test
    fun `no jump returns single server`() = runTest {
        coEvery { repo.getById("a") } returns server("a", null)
        val chain = useCase("a")
        assertEquals(1, chain.size)
        assertEquals("a", chain[0].id)
    }

    @Test
    fun `single jump chain`() = runTest {
        coEvery { repo.getById("target") } returns server("target", "bastion")
        coEvery { repo.getById("bastion") } returns server("bastion", null)
        val chain = useCase("target")
        assertEquals(2, chain.size)
        assertEquals("target", chain[0].id)
        assertEquals("bastion", chain[1].id)
    }

    @Test
    fun `two-hop chain`() = runTest {
        coEvery { repo.getById("t") } returns server("t", "j1")
        coEvery { repo.getById("j1") } returns server("j1", "j2")
        coEvery { repo.getById("j2") } returns server("j2", null)
        val chain = useCase("t")
        assertEquals(3, chain.size)
        assertEquals("t", chain[0].id)
        assertEquals("j1", chain[1].id)
        assertEquals("j2", chain[2].id)
    }

    @Test
    fun `detects loop`() = runTest {
        coEvery { repo.getById("a") } returns server("a", "b")
        coEvery { repo.getById("b") } returns server("b", "a")
        assertThrows(DomainError.InvalidInput::class.java) {
            kotlinx.coroutines.runBlocking { useCase("a") }
        }
    }

    @Test
    fun `missing server throws NotFound`() = runTest {
        coEvery { repo.getById("x") } returns null
        assertThrows(DomainError.NotFound::class.java) {
            kotlinx.coroutines.runBlocking { useCase("x") }
        }
    }

    @Test
    fun `detects too deep chain`() = runTest {
        // 10 уровней, лимит 5
        coEvery { repo.getById("s0") } returns server("s0", "s1")
        coEvery { repo.getById("s1") } returns server("s1", "s2")
        coEvery { repo.getById("s2") } returns server("s2", "s3")
        coEvery { repo.getById("s3") } returns server("s3", "s4")
        coEvery { repo.getById("s4") } returns server("s4", "s5")
        coEvery { repo.getById("s5") } returns server("s5", "s6")
        assertThrows(DomainError.InvalidInput::class.java) {
            kotlinx.coroutines.runBlocking { useCase("s0") }
        }
    }
}
