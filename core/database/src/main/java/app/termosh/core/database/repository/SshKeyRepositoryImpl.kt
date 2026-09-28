package app.termosh.core.database.repository

import app.termosh.core.database.dao.SshKeyDao
import app.termosh.core.database.mapper.SshKeyMapper
import app.termosh.core.database.mapper.SshKeyTypeBridge
import app.termosh.core.security.SshKeyStore
import app.termosh.core.security.model.SshKeyPair
import app.termosh.domain.model.SecretRef
import app.termosh.domain.model.SshKey
import app.termosh.domain.model.SshKeyType
import app.termosh.domain.repository.SshKeyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshKeyRepositoryImpl @Inject constructor(
    private val dao: SshKeyDao,
    private val keyStore: SshKeyStore,
) : SshKeyRepository {

    override fun observeAll(): Flow<List<SshKey>> =
        dao.observeAll().map { list -> list.map(SshKeyMapper::toDomain) }

    override suspend fun getById(id: String): SshKey? =
        dao.getById(id)?.let(SshKeyMapper::toDomain)

    override suspend fun generate(type: SshKeyType, comment: String?): SshKey {
        val securityType = SshKeyTypeBridge.toSecurity(type)
        val pair: SshKeyPair = keyStore.generateAndWrap(securityType, comment)

        val domain = SshKey(
            id = pair.id,
            type = type,
            privateKeyRef = SecretRef(
                iv = pair.encryptedPrivateKey.iv,
                ciphertext = pair.encryptedPrivateKey.ciphertext,
            ),
            publicKeyOpenSsh = pair.publicKeyOpenSsh,
            fingerprintSha256 = pair.fingerprintSha256,
            comment = pair.comment,
            createdAt = pair.createdAt,
        )
        dao.upsert(SshKeyMapper.toEntity(domain))
        return domain
    }

    override suspend fun save(key: SshKey) {
        dao.upsert(SshKeyMapper.toEntity(key))
    }

    override suspend fun getMany(ids: List<String>): List<SshKey> =
        ids.mapNotNull { dao.getById(it)?.let(SshKeyMapper::toDomain) }

    override suspend fun delete(id: String) = dao.deleteById(id)
}
