package app.termosh.core.database.di

import app.termosh.core.database.repository.KnownHostRepositoryImpl
import app.termosh.core.database.repository.ServerRepositoryImpl
import app.termosh.core.database.repository.SnippetRepositoryImpl
import app.termosh.core.database.repository.SshKeyRepositoryImpl
import app.termosh.core.database.repository.TotpRepositoryImpl
import app.termosh.domain.repository.KnownHostRepository
import app.termosh.domain.repository.SecretCodec
import app.termosh.domain.repository.ServerRepository
import app.termosh.domain.repository.SnippetRepository
import app.termosh.domain.repository.SshKeyRepository
import app.termosh.domain.repository.TotpRepository
import app.termosh.core.security.codec.CryptoSecretCodec
import app.termosh.core.database.repository.PortForwardRepositoryImpl
import app.termosh.domain.repository.PortForwardRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindServerRepository(impl: ServerRepositoryImpl): ServerRepository
    @Binds
abstract fun bindPortForwardRepository(impl: PortForwardRepositoryImpl): PortForwardRepository

    @Binds
    abstract fun bindSshKeyRepository(impl: SshKeyRepositoryImpl): SshKeyRepository

    @Binds
    abstract fun bindKnownHostRepository(impl: KnownHostRepositoryImpl): KnownHostRepository

    @Binds
    abstract fun bindSnippetRepository(impl: SnippetRepositoryImpl): SnippetRepository

    @Binds
    abstract fun bindTotpRepository(impl: TotpRepositoryImpl): TotpRepository

    @Binds
    abstract fun bindSecretCodec(impl: CryptoSecretCodec): SecretCodec
}
