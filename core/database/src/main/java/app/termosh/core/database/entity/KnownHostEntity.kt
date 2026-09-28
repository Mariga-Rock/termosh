package app.termosh.core.database.entity

import androidx.room.Entity

/**
 * Отпечаток SSH-ключа хоста для TOFU-проверки.
 * При первом подключении записывается, при последующих — сверяется.
 */
@Entity(
    tableName = "known_hosts",
    primaryKeys = ["host", "port"],
)
data class KnownHostEntity(
    val host: String,
    val port: Int,
    val fingerprintSha256: String,
    val addedAt: Long,
)
