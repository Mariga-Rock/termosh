package app.termosh.domain.repository

import app.termosh.domain.model.TotpSecret
import kotlinx.coroutines.flow.Flow

interface TotpRepository {
    fun observeAll(): Flow<List<TotpSecret>>
    suspend fun getById(id: String): TotpSecret?
    suspend fun save(secret: TotpSecret)
    suspend fun delete(id: String)
}
