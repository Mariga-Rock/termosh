package app.termosh.core.database.repository

import app.termosh.core.database.dao.ServerDao
import app.termosh.core.database.mapper.ServerMapper
import app.termosh.domain.model.Server
import app.termosh.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepositoryImpl @Inject constructor(
    private val dao: ServerDao,
) : ServerRepository {

    override fun observeAll(): Flow<List<Server>> =
        dao.observeAll().map { list -> list.map(ServerMapper::toDomain) }

    override suspend fun getById(id: String): Server? =
        dao.getById(id)?.let(ServerMapper::toDomain)

    override suspend fun save(server: Server) {
        dao.upsert(ServerMapper.toEntity(server))
    }

    override suspend fun delete(id: String) = dao.deleteById(id)

    override suspend fun markUsed(id: String, timestamp: Long) =
        dao.updateLastUsed(id, timestamp)

    override suspend fun reorder(ids: List<String>) {
        ids.forEachIndexed { index, id ->
            val existing = dao.getById(id) ?: return@forEachIndexed
            dao.upsert(existing.copy(sortOrder = index))
        }
    }
}
