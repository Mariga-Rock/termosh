package app.termosh.domain.usecase.server

import app.termosh.domain.model.DomainError
import app.termosh.domain.model.Server
import app.termosh.domain.repository.ServerRepository
import java.util.UUID

class SaveServerUseCase(
    private val repository: ServerRepository,
) {
    suspend operator fun invoke(server: Server): Server {
        validate(server)
        val normalized = if (server.id.isBlank()) {
            server.copy(id = UUID.randomUUID().toString(), createdAt = System.currentTimeMillis())
        } else server
        repository.save(normalized)
        return normalized
    }

    private fun validate(server: Server) {
        if (server.name.isBlank()) throw DomainError.InvalidInput("name is blank")
        if (server.host.isBlank()) throw DomainError.InvalidInput("host is blank")
        if (server.port !in 1..65535) throw DomainError.InvalidInput("port out of range: ${server.port}")
        if (server.username.isBlank()) throw DomainError.InvalidInput("username is blank")
    }
}
