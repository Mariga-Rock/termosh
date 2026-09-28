package app.termosh.domain.repository

import app.termosh.domain.model.KnownHost
import app.termosh.domain.model.KnownHostHashed

interface KnownHostRepository {

    suspend fun get(host: String, port: Int): KnownHost?

    /** Ищет совпадение в открытых записях, затем — в хешированных (HMAC-SHA1). */
    suspend fun findMatch(host: String, port: Int): KnownHost?

    suspend fun save(entry: KnownHost)

    suspend fun saveHashed(entry: KnownHostHashed)

    suspend fun delete(host: String, port: Int)

    suspend fun deleteAll()
}
