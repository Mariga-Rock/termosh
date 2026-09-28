package app.termosh.domain.repository

import app.termosh.domain.model.Server
import kotlinx.coroutines.flow.Flow

interface ServerRepository {

    fun observeAll(): Flow<List<Server>>

    suspend fun getById(id: String): Server?

    suspend fun save(server: Server)

    suspend fun delete(id: String)

    suspend fun markUsed(id: String, timestamp: Long)
    suspend fun reorder(ids: List<String>)
}
