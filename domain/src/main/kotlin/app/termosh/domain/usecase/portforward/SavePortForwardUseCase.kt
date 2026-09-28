package app.termosh.domain.usecase.portforward

import app.termosh.domain.model.DomainError
import app.termosh.domain.model.PortForward
import app.termosh.domain.repository.PortForwardRepository
import java.util.UUID

class SavePortForwardUseCase(private val repo: PortForwardRepository) {
    suspend operator fun invoke(pf: PortForward): PortForward {
        if (pf.localPort !in 1..65535) throw DomainError.InvalidInput("localPort out of range")
        if (pf.remotePort !in 1..65535) throw DomainError.InvalidInput("remotePort out of range")
        if (pf.remoteHost.isBlank()) throw DomainError.InvalidInput("remoteHost is blank")
        val norm = if (pf.id.isBlank()) pf.copy(id = UUID.randomUUID().toString(), createdAt = System.currentTimeMillis()) else pf
        repo.save(norm)
        return norm
    }
}
