package app.termosh.domain.usecase

import app.termosh.domain.model.DomainError
import app.termosh.domain.model.SecretRef
import app.termosh.domain.model.SshKey
import app.termosh.domain.model.SshKeyType
import app.termosh.domain.repository.SshKeyRepository
import app.termosh.domain.usecase.key.GenerateKeyUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GenerateKeyUseCaseTest {

    private val repo = mockk<SshKeyRepository>()

    private fun fakeKey() = SshKey(
        id = "k1",
        type = SshKeyType.ED25519,
        privateKeyRef = SecretRef(byteArrayOf(1), byteArrayOf(2)),
        publicKeyOpenSsh = "ssh-ed25519 AAAA",
        fingerprintSha256 = "SHA256:aaa",
        comment = null,
        createdAt = 1L,
    )

    @Test
    fun `delegates to repo`() = runTest {
        coEvery { repo.generate(any(), any()) } returns fakeKey()
        val r = GenerateKeyUseCase(repo)(SshKeyType.ED25519, null)
        assertEquals("k1", r.id)
    }

    @Test
    fun `rejects too long comment`() {
        assertThrows(DomainError.InvalidInput::class.java) {
            kotlinx.coroutines.runBlocking {
                GenerateKeyUseCase(repo)(SshKeyType.ED25519, "x".repeat(300))
            }
        }
    }

    @Test
    fun `accepts null comment`() = runTest {
        coEvery { repo.generate(any(), null) } returns fakeKey()
        GenerateKeyUseCase(repo)(SshKeyType.ED25519, null)
    }

    @Test
    fun `accepts short comment`() = runTest {
        coEvery { repo.generate(any(), any()) } returns fakeKey()
        GenerateKeyUseCase(repo)(SshKeyType.ED25519, "work key")
    }
}
