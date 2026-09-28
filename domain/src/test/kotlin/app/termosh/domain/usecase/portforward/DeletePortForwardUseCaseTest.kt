package app.termosh.domain.usecase.portforward

import app.termosh.domain.repository.PortForwardRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeletePortForwardUseCaseTest {

    private val repo = mockk<PortForwardRepository>(relaxed = true)

    @Test
    fun `delegates to repo`() = runTest {
        DeletePortForwardUseCase(repo)("pf-1")
        coVerify(exactly = 1) { repo.delete("pf-1") }
    }
}
