package app.termosh.domain.usecase.portforward

import app.termosh.domain.model.PortForward
import app.termosh.domain.repository.PortForwardRepository
import kotlinx.coroutines.flow.Flow

class ObservePortForwardsUseCase(private val repo: PortForwardRepository) {
    operator fun invoke(serverId: String): Flow<List<PortForward>> = repo.observeForServer(serverId)
}
