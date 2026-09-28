package app.termosh.domain.usecase.key

import app.termosh.domain.repository.SshKeyRepository

class DeleteKeyUseCase(
    private val repository: SshKeyRepository,
) {
    suspend operator fun invoke(id: String) = repository.delete(id)
}
