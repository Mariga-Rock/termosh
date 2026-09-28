package app.termosh.domain.repository

import app.termosh.domain.model.PortForward
import kotlinx.coroutines.flow.Flow

interface PortForwardRepository {
    fun observeForServer(serverId: String): Flow<List<PortForward>>
    suspend fun getById(id: String): PortForward?
    suspend fun save(pf: PortForward)
    suspend fun delete(id: String)
}
