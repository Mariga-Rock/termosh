package app.termosh.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import app.termosh.core.database.dao.KnownHostDao
import app.termosh.core.database.dao.KnownHostHashedDao
import app.termosh.core.database.dao.PortForwardDao
import app.termosh.core.database.dao.ServerDao
import app.termosh.core.database.dao.SnippetDao
import app.termosh.core.database.dao.SshKeyDao
import app.termosh.core.database.dao.TotpDao
import app.termosh.core.database.entity.KnownHostEntity
import app.termosh.core.database.entity.KnownHostHashedEntity
import app.termosh.core.database.entity.PortForwardEntity
import app.termosh.core.database.entity.ServerEntity
import app.termosh.core.database.entity.SnippetEntity
import app.termosh.core.database.entity.SshKeyEntity
import app.termosh.core.database.entity.TotpEntity

@Database(
    entities = [
        ServerEntity::class,
        SshKeyEntity::class,
        KnownHostEntity::class,
        KnownHostHashedEntity::class,
        SnippetEntity::class,
        PortForwardEntity::class,
        TotpEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class TermoshDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun sshKeyDao(): SshKeyDao
    abstract fun knownHostDao(): KnownHostDao
    abstract fun knownHostHashedDao(): KnownHostHashedDao
    abstract fun snippetDao(): SnippetDao
    abstract fun portForwardDao(): PortForwardDao
    abstract fun totpDao(): TotpDao
}
