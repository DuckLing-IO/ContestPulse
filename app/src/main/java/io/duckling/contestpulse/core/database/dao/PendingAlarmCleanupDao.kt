package io.duckling.contestpulse.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.duckling.contestpulse.core.database.entity.PendingAlarmCleanupEntity

@Dao
interface PendingAlarmCleanupDao {
    @Query("SELECT * FROM pending_alarm_cleanup ORDER BY createdAtEpochMillis, id")
    suspend fun getAll(): List<PendingAlarmCleanupEntity>

    @Upsert
    suspend fun upsert(entity: PendingAlarmCleanupEntity)

    @Upsert
    suspend fun upsertAll(entities: List<PendingAlarmCleanupEntity>)

    @Query("DELETE FROM pending_alarm_cleanup WHERE id = :id")
    suspend fun delete(id: String)
}
