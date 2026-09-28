package app.termosh.domain.usecase

import app.termosh.domain.repository.ServerRepository
import app.termosh.domain.usecase.server.DeleteServerUseCase
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteServerUseCaseTest {

    private val repo = mockk<ServerRepository>(relaxed = true)

    @Test
    fun `calls repo delete with id`() = runTest {
        DeleteServerUseCase(repo)("srv-1")
        coVerify(exactly = 1) { repo.delete("srv-1") }
    }

    @Test
    fun `passes empty id without error`() = runTest {
        DeleteServerUseCase(repo)("")
        coVerify(exactly = 1) { repo.delete("") }
    }
}
