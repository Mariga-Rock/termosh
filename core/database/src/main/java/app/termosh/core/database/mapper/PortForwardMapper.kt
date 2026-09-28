package app.termosh.core.database.mapper

import app.termosh.core.database.entity.PortForwardEntity
import app.termosh.domain.model.PortForward

object PortForwardMapper {
    fun toDomain(e: PortForwardEntity) = PortForward(
        id = e.id, serverId = e.serverId, localPort = e.localPort,
        remoteHost = e.remoteHost, remotePort = e.remotePort,
        enabled = e.enabled, createdAt = e.createdAt,
    )
    fun toEntity(d: PortForward) = PortForwardEntity(
        id = d.id, serverId = d.serverId, localPort = d.localPort,
        remoteHost = d.remoteHost, remotePort = d.remotePort,
        enabled = d.enabled, createdAt = d.createdAt,
    )
}
