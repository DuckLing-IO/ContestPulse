package io.duckling.contestpulse.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.duckling.contestpulse.core.database.ContestPulseDatabase
import io.duckling.contestpulse.core.database.MIGRATION_1_2
import io.duckling.contestpulse.core.database.dao.ContestDao
import io.duckling.contestpulse.core.database.dao.FavoriteDao
import io.duckling.contestpulse.core.database.dao.SyncStatusDao
import io.duckling.contestpulse.core.database.dao.ReminderDao
import io.duckling.contestpulse.core.database.dao.PendingAlarmCleanupDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): ContestPulseDatabase = Room.databaseBuilder(
        context,
        ContestPulseDatabase::class.java,
        DATABASE_NAME,
    ).addMigrations(MIGRATION_1_2).build()

    @Provides
    fun provideContestDao(database: ContestPulseDatabase): ContestDao = database.contestDao()

    @Provides
    fun provideFavoriteDao(database: ContestPulseDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun provideReminderDao(database: ContestPulseDatabase): ReminderDao = database.reminderDao()

    @Provides
    fun providePendingAlarmCleanupDao(database: ContestPulseDatabase): PendingAlarmCleanupDao =
        database.pendingAlarmCleanupDao()

    @Provides
    fun provideSyncStatusDao(database: ContestPulseDatabase): SyncStatusDao =
        database.syncStatusDao()
}

private const val DATABASE_NAME = "contest-pulse.db"
