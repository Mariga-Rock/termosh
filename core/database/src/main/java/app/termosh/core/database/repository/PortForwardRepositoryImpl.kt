package app.termosh.core.database.repository

import app.termosh.core.database.dao.PortForwardDao
import app.termosh.core.database.mapper.PortForwardMapper
import app.termosh.domain.model.PortForward
import app.termosh.domain.repository.PortForwardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortForwardRepositoryImpl @Inject constructor(
    private val dao: PortForwardDao,
) : PortForwardRepository {
    override fun observeForServer(serverId: String): Flow<List<PortForward>> =
        dao.observeForServer(serverId).map { list -> list.map(PortForwardMapper::toDomain) }

    override suspend fun getById(id: String): PortForward? =
        dao.getById(id)?.let(PortForwardMapper::toDomain)

    override suspend fun save(pf: PortForward) = dao.upsert(PortForwardMapper.toEntity(pf))

    override suspend fun delete(id: String) = dao.deleteById(id)
}
