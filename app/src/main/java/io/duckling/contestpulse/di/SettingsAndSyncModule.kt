package io.duckling.contestpulse.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.duckling.contestpulse.data.settings.DataStoreSettingsRepository
import io.duckling.contestpulse.data.customsource.DataStoreCustomSourceRepository
import io.duckling.contestpulse.domain.customsource.CustomSourceRepository
import io.duckling.contestpulse.domain.settings.SettingsRepository
import io.duckling.contestpulse.sync.SyncWorkScheduler
import io.duckling.contestpulse.sync.WorkManagerSyncWorkScheduler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsAndSyncModule {
    @Binds
    @Singleton
    abstract fun bindCustomSourceRepository(
        repository: DataStoreCustomSourceRepository,
    ): CustomSourceRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        repository: DataStoreSettingsRepository,
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSyncWorkScheduler(
        scheduler: WorkManagerSyncWorkScheduler,
    ): SyncWorkScheduler

    companion object {
        @Provides
        @Singleton
        fun provideWorkManager(
            @ApplicationContext context: Context,
        ): WorkManager = WorkManager.getInstance(context)
    }
}
