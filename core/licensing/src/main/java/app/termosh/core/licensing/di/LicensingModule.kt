package app.termosh.core.licensing.di

import app.termosh.core.licensing.DeviceIdentityProvider
import app.termosh.core.licensing.LicenseRepository
import app.termosh.core.licensing.LicenseVerifier
import app.termosh.core.security.SshKeyStore
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LicensingModule {

    @Provides
    @Singleton
    fun provideDeviceIdentity(
        @ApplicationContext context: Context,
        keyStore: SshKeyStore,
    ): DeviceIdentityProvider = DeviceIdentityProvider(context, keyStore)

    @Provides
    @Singleton
    fun provideLicenseVerifier(device: DeviceIdentityProvider): LicenseVerifier =
        LicenseVerifier(device)

    @Provides
    @Singleton
    fun provideLicenseRepository(
        @ApplicationContext context: Context,
        verifier: LicenseVerifier,
    ): LicenseRepository = LicenseRepository(context, verifier)
}
