package app.termosh.domain.usecase

import app.termosh.domain.model.KnownHost
import app.termosh.domain.repository.KnownHostRepository
import app.termosh.domain.usecase.knownhost.VerifyHostKeyUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VerifyHostKeyUseCaseTest {

    private val repository = mockk<KnownHostRepository>()
    private val useCase = VerifyHostKeyUseCase(repository)

    @Test
    fun `accepts and stores on first use`() = runTest {
        coEvery { repository.get("h", 22) } returns null
        val saved = slot<KnownHost>()
        coEvery { repository.save(capture(saved)) } returns Unit

        val result = useCase("h", 22, "SHA256:abc")

        assertTrue(result is VerifyHostKeyUseCase.Result.AcceptFirstTime)
        assertEquals("SHA256:abc", saved.captured.fingerprintSha256)
    }

    @Test
    fun `accepts on matching fingerprint`() = runTest {
        coEvery { repository.get("h", 22) } returns
            KnownHost("h", 22, "SHA256:abc", 0L)

        val result = useCase("h", 22, "SHA256:abc")
        assertTrue(result is VerifyHostKeyUseCase.Result.Accept)
    }

    @Test
    fun `rejects on mismatch`() = runTest {
        coEvery { repository.get("h", 22) } returns
            KnownHost("h", 22, "SHA256:old", 0L)

        val result = useCase("h", 22, "SHA256:new")
        assertTrue(result is VerifyHostKeyUseCase.Result.Reject)
        assertEquals("SHA256:old", (result as VerifyHostKeyUseCase.Result.Reject).expected)
        assertEquals("SHA256:new", result.actual)
        coVerify(exactly = 0) { repository.save(any()) }
    }
}
