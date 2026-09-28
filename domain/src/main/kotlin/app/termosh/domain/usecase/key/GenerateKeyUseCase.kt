package app.termosh.domain.usecase.key

import app.termosh.domain.model.DomainError
import app.termosh.domain.model.SshKey
import app.termosh.domain.model.SshKeyType
import app.termosh.domain.repository.SshKeyRepository

class GenerateKeyUseCase(
    private val repository: SshKeyRepository,
) {
    suspend operator fun invoke(type: SshKeyType, comment: String?): SshKey {
        if (comment != null && comment.length > 200) {
            throw DomainError.InvalidInput("comment too long")
        }
        return repository.generate(type, comment)
    }
}
