package app.termosh.domain.usecase.connection

import app.termosh.domain.model.DomainError
import app.termosh.domain.model.Server
import app.termosh.domain.repository.ServerRepository

/**
 * Возвращает цепочку серверов для ProxyJump: [origin, jump1, jump2, ...].
 * Простая защита от циклов и слишком глубоких цепочек.
 */
class GetProxyJumpChainUseCase(
    private val repository: ServerRepository,
) {
    companion object {
        private const val MAX_DEPTH = 5
    }

    suspend operator fun invoke(serverId: String): List<Server> {
        val chain = mutableListOf<Server>()
        val visited = mutableSetOf<String>()
        var currentId: String? = serverId

        while (currentId != null) {
            if (!visited.add(currentId)) {
                throw DomainError.InvalidInput("ProxyJump loop detected at $currentId")
            }
            if (chain.size >= MAX_DEPTH) {
                throw DomainError.InvalidInput("ProxyJump chain too deep (>$MAX_DEPTH)")
            }
            val server = repository.getById(currentId)
                ?: throw DomainError.NotFound("server:$currentId")
            chain += server
            currentId = server.proxyJumpId
        }

        return chain
    }
}
