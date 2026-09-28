package app.termosh.core.database.mapper

import app.termosh.core.database.entity.KnownHostEntity
import app.termosh.domain.model.KnownHost

object KnownHostMapper {

    fun toDomain(entity: KnownHostEntity): KnownHost = KnownHost(
        host = entity.host,
        port = entity.port,
        fingerprintSha256 = entity.fingerprintSha256,
        addedAt = entity.addedAt,
    )

    fun toEntity(domain: KnownHost): KnownHostEntity = KnownHostEntity(
        host = domain.host,
        port = domain.port,
        fingerprintSha256 = domain.fingerprintSha256,
        addedAt = domain.addedAt,
    )
}
