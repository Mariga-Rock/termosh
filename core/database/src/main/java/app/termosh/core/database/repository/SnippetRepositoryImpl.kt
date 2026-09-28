package app.termosh.core.database.repository

import app.termosh.core.database.dao.SnippetDao
import app.termosh.core.database.mapper.SnippetMapper
import app.termosh.domain.model.Snippet
import app.termosh.domain.repository.SnippetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnippetRepositoryImpl @Inject constructor(
    private val dao: SnippetDao,
) : SnippetRepository {

    override fun observeAll(): Flow<List<Snippet>> =
        dao.observeAll().map { list -> list.map(SnippetMapper::toDomain) }

    override suspend fun save(snippet: Snippet) =
        dao.upsert(SnippetMapper.toEntity(snippet))

    override suspend fun delete(id: String) {
        // TODO: добавить метод deleteById в SnippetDao
    }
}
