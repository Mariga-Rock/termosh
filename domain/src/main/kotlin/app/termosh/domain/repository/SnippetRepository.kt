package app.termosh.domain.repository

import app.termosh.domain.model.Snippet
import kotlinx.coroutines.flow.Flow

interface SnippetRepository {

    fun observeAll(): Flow<List<Snippet>>

    suspend fun save(snippet: Snippet)

    suspend fun delete(id: String)
}
