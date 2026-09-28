package app.termosh.domain.usecase

import app.termosh.domain.model.DomainError
import app.termosh.domain.model.Snippet
import app.termosh.domain.repository.SnippetRepository
import app.termosh.domain.usecase.snippet.SaveSnippetUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveSnippetUseCaseTest {

    private val repo = mockk<SnippetRepository>(relaxed = true)

    @Test
    fun `assigns id when blank`() = runTest {
        coEvery { repo.save(any()) } returns Unit
        val r = SaveSnippetUseCase(repo)(
            Snippet(id = "", name = "n", command = "ls", sortOrder = 0, createdAt = 0L),
        )
        assertTrue(r.id.isNotBlank())
    }

    @Test
    fun `rejects blank name`() {
        assertThrows(DomainError.InvalidInput::class.java) {
            kotlinx.coroutines.runBlocking {
                SaveSnippetUseCase(repo)(
                    Snippet(id = "", name = "", command = "ls", sortOrder = 0, createdAt = 0L),
                )
            }
        }
    }

    @Test
    fun `rejects blank command`() {
        assertThrows(DomainError.InvalidInput::class.java) {
            kotlinx.coroutines.runBlocking {
                SaveSnippetUseCase(repo)(
                    Snippet(id = "", name = "n", command = "", sortOrder = 0, createdAt = 0L),
                )
            }
        }
    }
}
