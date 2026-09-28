package app.termosh.domain.repository

import app.termosh.domain.model.SshKey
import app.termosh.domain.model.SshKeyType
import kotlinx.coroutines.flow.Flow

interface SshKeyRepository {

    fun observeAll(): Flow<List<SshKey>>

    suspend fun getById(id: String): SshKey?

    /**
     * Генерирует новую пару в Keystore-обёртке и сохраняет её.
     * Возвращает доменную модель.
     */
    suspend fun generate(type: SshKeyType, comment: String?): SshKey

    suspend fun save(key: SshKey)
    suspend fun getMany(ids: List<String>): List<SshKey>
    suspend fun delete(id: String)
}
