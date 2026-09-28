package app.termosh.core.database.mapper

import app.termosh.core.database.entity.SshKeyEntity
import app.termosh.domain.model.SecretRef
import app.termosh.domain.model.SshKey
import app.termosh.domain.model.SshKeyType

object SshKeyMapper {

    fun toDomain(entity: SshKeyEntity): SshKey = SshKey(
        id = entity.id,
        type = SshKeyType.valueOf(entity.type),
        privateKeyRef = SecretRef(
            iv = entity.encryptedPrivateKeyIv,
            ciphertext = entity.encryptedPrivateKeyCiphertext,
        ),
        publicKeyOpenSsh = entity.publicKeyOpenSsh,
        fingerprintSha256 = entity.fingerprintSha256,
        comment = entity.comment,
        createdAt = entity.createdAt,
    )

    fun toEntity(domain: SshKey): SshKeyEntity = SshKeyEntity(
        id = domain.id,
        type = domain.type.name,
        encryptedPrivateKeyIv = domain.privateKeyRef.iv,
        encryptedPrivateKeyCiphertext = domain.privateKeyRef.ciphertext,
        publicKeyOpenSsh = domain.publicKeyOpenSsh,
        fingerprintSha256 = domain.fingerprintSha256,
        comment = domain.comment,
        createdAt = domain.createdAt,
    )
}
