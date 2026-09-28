package app.termosh.domain.usecase.totp

import app.termosh.domain.model.TotpSecret
import app.termosh.domain.repository.TotpRepository
import kotlinx.coroutines.flow.Flow

class ObserveTotpUseCase(private val repo: TotpRepository) {
    operator fun invoke(): Flow<List<TotpSecret>> = repo.observeAll()
}
