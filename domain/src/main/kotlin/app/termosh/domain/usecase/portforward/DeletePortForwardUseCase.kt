package app.termosh.domain.usecase.portforward

import app.termosh.domain.repository.PortForwardRepository

class DeletePortForwardUseCase(private val repo: PortForwardRepository) {
    suspend operator fun invoke(id: String) = repo.delete(id)
}
