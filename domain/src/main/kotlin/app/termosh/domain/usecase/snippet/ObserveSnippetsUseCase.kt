package app.termosh.domain.usecase.snippet

import app.termosh.domain.model.Snippet
import app.termosh.domain.repository.SnippetRepository
import kotlinx.coroutines.flow.Flow

class ObserveSnippetsUseCase(
    private val repository: SnippetRepository,
) {
    operator fun invoke(): Flow<List<Snippet>> = repository.observeAll()
}
