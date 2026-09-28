package app.termosh.di

import app.termosh.domain.repository.KnownHostRepository
import app.termosh.domain.repository.SecretCodec
import app.termosh.domain.repository.ServerRepository
import app.termosh.domain.repository.SnippetRepository
import app.termosh.domain.repository.SshKeyRepository
import app.termosh.domain.usecase.connection.BuildConnectionRequestUseCase
import app.termosh.domain.usecase.connection.GetProxyJumpChainUseCase
import app.termosh.domain.usecase.key.DeleteKeyUseCase
import app.termosh.domain.usecase.key.GenerateKeyUseCase
import app.termosh.domain.usecase.key.ObserveKeysUseCase
import app.termosh.domain.usecase.knownhost.VerifyHostKeyUseCase
import app.termosh.domain.usecase.server.DeleteServerUseCase
import app.termosh.domain.usecase.server.GetServerUseCase
import app.termosh.domain.usecase.server.ObserveServersUseCase
import app.termosh.domain.usecase.server.ReorderServersUseCase
import app.termosh.domain.usecase.server.SaveServerUseCase
import app.termosh.domain.usecase.snippet.ObserveSnippetsUseCase
import app.termosh.domain.usecase.snippet.SaveSnippetUseCase
import app.termosh.domain.repository.PortForwardRepository
import app.termosh.domain.repository.TotpRepository
import app.termosh.domain.usecase.totp.ObserveTotpUseCase
import app.termosh.domain.usecase.totp.SaveTotpUseCase
import app.termosh.domain.usecase.totp.DeleteTotpUseCase
import app.termosh.domain.usecase.portforward.ObservePortForwardsUseCase
import app.termosh.domain.usecase.portforward.SavePortForwardUseCase
import app.termosh.domain.usecase.portforward.DeletePortForwardUseCase
import app.termosh.domain.usecase.portforward.GetEnabledPortForwardsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Предоставляет все Use Cases из :domain.
 * :domain — чистый Kotlin без Hilt, поэтому связывание делается здесь.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ---- Server ----

    @Provides
    @Singleton
    fun provideObserveServersUseCase(repo: ServerRepository): ObserveServersUseCase =
        ObserveServersUseCase(repo)

    @Provides
    @Singleton
    fun provideGetServerUseCase(repo: ServerRepository): GetServerUseCase =
        GetServerUseCase(repo)

    @Provides
    @Singleton
    fun provideSaveServerUseCase(repo: ServerRepository): SaveServerUseCase =
        SaveServerUseCase(repo)

    @Provides
    @Singleton
    fun provideDeleteServerUseCase(repo: ServerRepository): DeleteServerUseCase =
        DeleteServerUseCase(repo)

    // ---- Key ----

    @Provides
    @Singleton
    fun provideObserveKeysUseCase(repo: SshKeyRepository): ObserveKeysUseCase =
        ObserveKeysUseCase(repo)

    @Provides
    @Singleton
    fun provideGenerateKeyUseCase(repo: SshKeyRepository): GenerateKeyUseCase =
        GenerateKeyUseCase(repo)

    @Provides
    @Singleton
    fun provideDeleteKeyUseCase(repo: SshKeyRepository): DeleteKeyUseCase =
        DeleteKeyUseCase(repo)

    // ---- Snippet ----

    @Provides
    @Singleton
    fun provideObserveSnippetsUseCase(repo: SnippetRepository): ObserveSnippetsUseCase =
        ObserveSnippetsUseCase(repo)

    @Provides
    @Singleton
    fun provideSaveSnippetUseCase(repo: SnippetRepository): SaveSnippetUseCase =
        SaveSnippetUseCase(repo)

    // ---- KnownHost ----

    @Provides
    @Singleton
    fun provideVerifyHostKeyUseCase(repo: KnownHostRepository): VerifyHostKeyUseCase =
        VerifyHostKeyUseCase(repo)

    // ---- Connection ----

    @Provides
    @Singleton
    fun provideBuildConnectionRequestUseCase(
        serverRepository: ServerRepository,
        keyRepository: SshKeyRepository,
        codec: SecretCodec,
    ): BuildConnectionRequestUseCase =
        BuildConnectionRequestUseCase(serverRepository, keyRepository, codec)

    @Provides
    @Singleton
    fun provideGetProxyJumpChainUseCase(repo: ServerRepository): GetProxyJumpChainUseCase =
        GetProxyJumpChainUseCase(repo)
    @Provides
    @Singleton
    fun provideObservePortForwardsUseCase(repo: PortForwardRepository): ObservePortForwardsUseCase =
        ObservePortForwardsUseCase(repo)

    @Provides
    @Singleton
    fun provideSavePortForwardUseCase(repo: PortForwardRepository): SavePortForwardUseCase =
        SavePortForwardUseCase(repo)

    @Provides
    @Singleton
    fun provideDeletePortForwardUseCase(repo: PortForwardRepository): DeletePortForwardUseCase =
        DeletePortForwardUseCase(repo)

    @Provides
    @Singleton
    fun provideGetEnabledPortForwardsUseCase(repo: PortForwardRepository): GetEnabledPortForwardsUseCase =
        GetEnabledPortForwardsUseCase(repo)


    @Provides
    @Singleton
    fun provideReorderServersUseCase(repo: ServerRepository): ReorderServersUseCase =
        ReorderServersUseCase(repo)


    @Provides
    @Singleton
    fun provideObserveTotpUseCase(repo: TotpRepository): ObserveTotpUseCase =
        ObserveTotpUseCase(repo)

    @Provides
    @Singleton
    fun provideSaveTotpUseCase(repo: TotpRepository): SaveTotpUseCase =
        SaveTotpUseCase(repo)

    @Provides
    @Singleton
    fun provideDeleteTotpUseCase(repo: TotpRepository): DeleteTotpUseCase =
        DeleteTotpUseCase(repo)

}
