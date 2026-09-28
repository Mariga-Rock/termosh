package app.termosh.domain.usecase.totp

import app.termosh.domain.model.DomainError
import app.termosh.domain.model.TotpSecret
import app.termosh.domain.repository.TotpRepository
import java.util.UUID

class SaveTotpUseCase(private val repo: TotpRepository) {
    suspend operator fun invoke(secret: TotpSecret): TotpSecret {
        if (secret.label.isBlank()) throw DomainError.InvalidInput("label is blank")
        val norm = if (secret.id.isBlank())
            secret.copy(id = UUID.randomUUID().toString(), createdAt = System.currentTimeMillis())
        else secret
        repo.save(norm)
        return norm
    }
}
