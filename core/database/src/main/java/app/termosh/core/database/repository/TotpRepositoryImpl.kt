package app.termosh.core.database.repository

import app.termosh.core.database.dao.TotpDao
import app.termosh.core.database.entity.TotpEntity
import app.termosh.core.security.CryptoManager
import app.termosh.core.security.model.EncryptedData
import app.termosh.domain.model.SecretRef
import app.termosh.domain.model.TotpSecret
import app.termosh.domain.repository.TotpRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TotpRepositoryImpl @Inject constructor(
    private val dao: TotpDao,
    private val crypto: CryptoManager,
) : TotpRepository {

    override fun observeAll(): Flow<List<TotpSecret>> =
        dao.observeAll().map { list -> list.map(::toDomain) }

    override suspend fun getById(id: String): TotpSecret? =
        dao.getById(id)?.let(::toDomain)

    override suspend fun save(secret: TotpSecret) {
        dao.upsert(
            TotpEntity(
                id = secret.id,
                label = secret.label,
                issuer = secret.issuer,
                encryptedSecretIv = secret.secretRef.iv,
                encryptedSecretCiphertext = secret.secretRef.ciphertext,
                digits = secret.digits,
                periodSec = secret.periodSec,
                algorithm = secret.algorithm,
                createdAt = secret.createdAt,
            ),
        )
    }

    override suspend fun delete(id: String) = dao.deleteById(id)

    private fun toDomain(e: TotpEntity) = TotpSecret(
        id = e.id,
        label = e.label,
        issuer = e.issuer,
        secretRef = SecretRef(e.encryptedSecretIv, e.encryptedSecretCiphertext),
        digits = e.digits,
        periodSec = e.periodSec,
        algorithm = e.algorithm,
        createdAt = e.createdAt,
    )
}
