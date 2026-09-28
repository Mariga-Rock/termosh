package app.termosh.domain.usecase.key

import app.termosh.domain.model.SshKey
import app.termosh.domain.repository.SshKeyRepository
import kotlinx.coroutines.flow.Flow

class ObserveKeysUseCase(
    private val repository: SshKeyRepository,
) {
    operator fun invoke(): Flow<List<SshKey>> = repository.observeAll()
}
