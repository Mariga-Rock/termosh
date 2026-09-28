package app.termosh.domain.usecase.server

import app.termosh.domain.repository.ServerRepository

class ReorderServersUseCase(private val repo: ServerRepository) {
    suspend operator fun invoke(ids: List<String>) = repo.reorder(ids)
}
