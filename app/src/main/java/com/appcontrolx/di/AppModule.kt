package com.appcontrolx.di

import android.content.Context
import com.appcontrolx.core.ShellManager
import com.appcontrolx.domain.AppManager
import com.appcontrolx.domain.AppScanner
import com.appcontrolx.domain.SafetyValidator
import com.appcontrolx.domain.SystemMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideShellManager(@ApplicationContext context: Context): ShellManager {
        return ShellManager(context)
    }

    @Provides
    @Singleton
    fun provideSafetyValidator(): SafetyValidator {
        return SafetyValidator()
    }

    @Provides
    @Singleton
    fun provideAppScanner(
        @ApplicationContext context: Context,
        shellManager: ShellManager
    ): AppScanner {
        return AppScanner(context, shellManager)
    }

    @Provides
    @Singleton
    fun provideAppManager(
        shellManager: ShellManager,
        safetyValidator: SafetyValidator
    ): AppManager {
        return AppManager(shellManager, safetyValidator)
    }

    @Provides
    @Singleton
    fun provideSystemMonitor(@ApplicationContext context: Context): SystemMonitor {
        return SystemMonitor(context)
    }
}
