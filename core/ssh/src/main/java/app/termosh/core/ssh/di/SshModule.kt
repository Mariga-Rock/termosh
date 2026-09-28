package app.termosh.core.ssh.di

import app.termosh.core.ssh.DockerManager
import app.termosh.core.ssh.FingerprintCalculator
import app.termosh.core.ssh.PortForwardManager
import app.termosh.core.ssh.SftpManager
import app.termosh.core.ssh.SshSessionManager
import app.termosh.core.ssh.model.SshSessionState
import app.termosh.core.ssh.verifier.TofuHostKeyVerifier
import app.termosh.core.security.TotpGenerator
import app.termosh.domain.repository.KnownHostRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SshModule {

    @Provides
    @Singleton
    fun provideSshSessionManager(
        knownHosts: KnownHostRepository,
        totpGenerator: TotpGenerator,
    ): SshSessionManager = SshSessionManager(knownHosts, totpGenerator)

    @Provides
    @Singleton
    fun provideFingerprintCalculator(): FingerprintCalculator = FingerprintCalculator()

    @Provides
    @Singleton
    fun providePortForwardManager(): PortForwardManager = PortForwardManager()

    @Provides
    @Singleton
    fun provideSftpManager(): SftpManager = SftpManager()

    @Provides
    @Singleton
    fun provideDockerManager(): DockerManager = DockerManager()
}
