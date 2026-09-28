package app.termosh.core.mosh.di

import app.termosh.core.mosh.MoshManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MoshModule {

    @Provides
    @Singleton
    fun provideMoshManager(@ApplicationContext context: Context): MoshManager =
        MoshManager(context)
}
