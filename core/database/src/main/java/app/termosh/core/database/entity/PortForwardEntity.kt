package app.termosh.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "port_forwards",
    indices = [Index("serverId")],
)
data class PortForwardEntity(
    @PrimaryKey val id: String,
    val serverId: String,
    val localPort: Int,
    val remoteHost: String,
    val remotePort: Int,
    val enabled: Boolean,
    val createdAt: Long,
)
