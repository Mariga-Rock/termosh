package app.termosh.core.database.di

import android.content.Context
import androidx.room.Room
import app.termosh.core.database.DatabaseKeyProvider
import app.termosh.core.database.Migrations
import app.termosh.core.database.TermoshDatabase
import app.termosh.core.database.dao.KnownHostDao
import app.termosh.core.database.dao.KnownHostHashedDao
import app.termosh.core.database.dao.PortForwardDao
import app.termosh.core.database.dao.ServerDao
import app.termosh.core.database.dao.SnippetDao
import app.termosh.core.database.dao.SshKeyDao
import app.termosh.core.database.dao.TotpDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DB_NAME = "termosh.db"

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyProvider: DatabaseKeyProvider,
    ): TermoshDatabase {
        System.loadLibrary("sqlcipher")
        val passphrase = keyProvider.getOrCreateKey()
        val factory = SupportOpenHelperFactory(passphrase)
        return Room.databaseBuilder(context, TermoshDatabase::class.java, DB_NAME)
            .openHelperFactory(factory)
            .addMigrations(*Migrations.ALL)
            .fallbackToDestructiveMigrationFrom(1, 2)
            .build()
    }

    @Provides fun provideServerDao(db: TermoshDatabase): ServerDao = db.serverDao()
    @Provides fun provideSshKeyDao(db: TermoshDatabase): SshKeyDao = db.sshKeyDao()
    @Provides fun provideKnownHostDao(db: TermoshDatabase): KnownHostDao = db.knownHostDao()
    @Provides fun provideKnownHostHashedDao(db: TermoshDatabase): KnownHostHashedDao = db.knownHostHashedDao()
    @Provides fun provideSnippetDao(db: TermoshDatabase): SnippetDao = db.snippetDao()
    @Provides fun providePortForwardDao(db: TermoshDatabase): PortForwardDao = db.portForwardDao()
    @Provides fun provideTotpDao(db: TermoshDatabase): TotpDao = db.totpDao()
}
