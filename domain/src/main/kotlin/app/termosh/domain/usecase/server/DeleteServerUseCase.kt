package app.termosh.domain.usecase.server

import app.termosh.domain.repository.ServerRepository

class DeleteServerUseCase(
    private val repository: ServerRepository,
) {
    suspend operator fun invoke(id: String) = repository.delete(id)
}
