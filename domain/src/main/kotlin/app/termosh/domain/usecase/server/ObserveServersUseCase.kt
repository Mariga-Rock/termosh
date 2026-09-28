package app.termosh.domain.usecase.server

import app.termosh.domain.model.Server
import app.termosh.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow

class ObserveServersUseCase(
    private val repository: ServerRepository,
) {
    operator fun invoke(): Flow<List<Server>> = repository.observeAll()
}
