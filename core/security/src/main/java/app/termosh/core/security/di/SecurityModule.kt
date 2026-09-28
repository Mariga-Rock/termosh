package app.termosh.core.security.di

import app.termosh.core.security.CryptoManager
import app.termosh.core.security.KeystoreManager
import app.termosh.core.security.SshKeyPairGenerator
import app.termosh.core.security.SshKeyStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideKeystoreManager(): KeystoreManager = KeystoreManager()

    @Provides
    @Singleton
    fun provideCryptoManager(keystore: KeystoreManager): CryptoManager = CryptoManager(keystore)

    @Provides
    @Singleton
    fun provideSshKeyPairGenerator(): SshKeyPairGenerator = SshKeyPairGenerator()

    @Provides
    @Singleton
    fun provideSshKeyStore(
        crypto: CryptoManager,
        generator: SshKeyPairGenerator,
    ): SshKeyStore = SshKeyStore(crypto, generator)
}
