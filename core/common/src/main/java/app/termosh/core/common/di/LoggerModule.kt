package app.termosh.core.common.di

import android.content.Context
import app.termosh.core.common.AppLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoggerModule {

    @Provides
    @Singleton
    fun provideAppLogger(@ApplicationContext context: Context): AppLogger {
        // Если Application.init() уже создал экземпляр — используем его.
        val existing = AppLogger.logFile()?.let { null } // трюк чтобы дёрнуть instance
        // Проще: создаём и сетим. setInstance не перезапишет уже установленный.
        val logger = AppLogger(context)
        AppLogger.setInstance(logger)
        return AppLogger.logFile()?.let { logger } ?: logger
    }
}
