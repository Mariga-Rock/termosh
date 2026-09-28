package app.termosh.domain.usecase.snippet

import app.termosh.domain.model.DomainError
import app.termosh.domain.model.Snippet
import app.termosh.domain.repository.SnippetRepository
import java.util.UUID

class SaveSnippetUseCase(
    private val repository: SnippetRepository,
) {
    suspend operator fun invoke(snippet: Snippet): Snippet {
        if (snippet.name.isBlank()) throw DomainError.InvalidInput("snippet name is blank")
        if (snippet.command.isBlank()) throw DomainError.InvalidInput("snippet command is blank")
        val normalized = if (snippet.id.isBlank()) {
            snippet.copy(id = UUID.randomUUID().toString(), createdAt = System.currentTimeMillis())
        } else snippet
        repository.save(normalized)
        return normalized
    }
}
