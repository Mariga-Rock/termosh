package app.termosh.core.ssh.di

import app.termosh.core.ssh.FingerprintCalculator
import app.termosh.core.ssh.SshManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SshModule {

    @Provides
    @Singleton
    fun provideSshManager(): SshManager = SshManager()

    @Provides
    @Singleton
    fun provideFingerprintCalculator(): FingerprintCalculator = FingerprintCalculator()
}
