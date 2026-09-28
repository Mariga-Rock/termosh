package app.termosh.domain.usecase.portforward

import app.termosh.domain.model.PortForward
import app.termosh.domain.repository.PortForwardRepository
import kotlinx.coroutines.flow.first

class GetEnabledPortForwardsUseCase(private val repo: PortForwardRepository) {
    suspend operator fun invoke(serverId: String): List<PortForward> =
        repo.observeForServer(serverId).first().filter { it.enabled }
}
