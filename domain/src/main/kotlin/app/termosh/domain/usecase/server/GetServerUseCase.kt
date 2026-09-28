package app.termosh.domain.usecase.server

import app.termosh.domain.model.Server
import app.termosh.domain.repository.ServerRepository
import app.termosh.domain.model.DomainError

class GetServerUseCase(
    private val repository: ServerRepository,
) {
    suspend operator fun invoke(id: String): Server =
        repository.getById(id) ?: throw DomainError.NotFound("server:$id")
}
