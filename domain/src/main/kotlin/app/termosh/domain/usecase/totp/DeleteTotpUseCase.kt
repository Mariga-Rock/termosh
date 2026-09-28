package app.termosh.domain.usecase.totp

import app.termosh.domain.repository.TotpRepository

class DeleteTotpUseCase(private val repo: TotpRepository) {
    suspend operator fun invoke(id: String) = repo.delete(id)
}
