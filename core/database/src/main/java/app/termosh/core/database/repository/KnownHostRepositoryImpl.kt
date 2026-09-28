package app.termosh.core.database.repository

import app.termosh.core.database.dao.KnownHostDao
import app.termosh.core.database.dao.KnownHostHashedDao
import app.termosh.core.database.entity.KnownHostHashedEntity
import app.termosh.core.database.mapper.KnownHostMapper
import app.termosh.domain.model.KnownHost
import app.termosh.domain.model.KnownHostHashed
import app.termosh.domain.repository.KnownHostRepository
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnownHostRepositoryImpl @Inject constructor(
    private val dao: KnownHostDao,
    private val hashedDao: KnownHostHashedDao,
) : KnownHostRepository {

    override suspend fun get(host: String, port: Int): KnownHost? =
        dao.get(host, port)?.let(KnownHostMapper::toDomain)

    override suspend fun findMatch(host: String, port: Int): KnownHost? {
        dao.get(host, port)?.let { return KnownHostMapper.toDomain(it) }

        val canonical = if (port == 22) host else "[$host]:$port"
        val canonicalBytes = canonical.toByteArray(Charsets.UTF_8)

        for (e in hashedDao.getAll()) {
            val salt = runCatching { Base64.getDecoder().decode(e.salt) }.getOrNull() ?: continue
            val mac = Mac.getInstance("HmacSHA1").apply {
                init(SecretKeySpec(salt, "HmacSHA1"))
            }
            val computed = Base64.getEncoder().encodeToString(mac.doFinal(canonicalBytes))
            if (computed == e.hash) {
                return KnownHost(
                    host = host,
                    port = port,
                    fingerprintSha256 = e.fingerprintSha256,
                    addedAt = e.addedAt,
                )
            }
        }
        return null
    }

    override suspend fun save(entry: KnownHost) =
        dao.upsert(KnownHostMapper.toEntity(entry))

    override suspend fun saveHashed(entry: KnownHostHashed) =
        hashedDao.upsert(
            KnownHostHashedEntity(
                salt = entry.salt,
                hash = entry.hash,
                keyType = entry.keyType,
                keyBase64 = entry.keyBase64,
                fingerprintSha256 = entry.fingerprintSha256,
                addedAt = entry.addedAt,
            )
        )

    override suspend fun delete(host: String, port: Int) = dao.delete(host, port)

    override suspend fun deleteAll() {
        dao.deleteAll()
        hashedDao.deleteAll()
    }
}
