package io.duckling.contestpulse.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.duckling.contestpulse.core.database.dao.ContestDao
import io.duckling.contestpulse.core.database.dao.FavoriteDao
import io.duckling.contestpulse.core.database.dao.SyncStatusDao
import io.duckling.contestpulse.core.database.dao.ReminderDao
import io.duckling.contestpulse.core.database.dao.PendingAlarmCleanupDao
import io.duckling.contestpulse.core.database.entity.ContestEntity
import io.duckling.contestpulse.core.database.entity.FavoriteEntity
import io.duckling.contestpulse.core.database.entity.ReminderEntity
import io.duckling.contestpulse.core.database.entity.PendingAlarmCleanupEntity
import io.duckling.contestpulse.core.database.entity.SyncStatusEntity

@Database(
    entities = [
        ContestEntity::class,
        FavoriteEntity::class,
        ReminderEntity::class,
        PendingAlarmCleanupEntity::class,
        SyncStatusEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class ContestPulseDatabase : RoomDatabase() {
    abstract fun contestDao(): ContestDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun reminderDao(): ReminderDao
    abstract fun pendingAlarmCleanupDao(): PendingAlarmCleanupDao
    abstract fun syncStatusDao(): SyncStatusDao
}
