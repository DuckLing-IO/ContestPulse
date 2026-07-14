package io.duckling.contestpulse.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.duckling.contestpulse.core.database.entity.ReminderEntity

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE id = :reminderId LIMIT 1")
    suspend fun getById(reminderId: String): ReminderEntity?

    @Query(
        """
        SELECT * FROM reminders
        WHERE contestId = :contestId AND offsetMinutes = :offsetMinutes
        LIMIT 1
        """,
    )
    suspend fun getByContestAndOffset(
        contestId: String,
        offsetMinutes: Long,
    ): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE contestId = :contestId AND isEnabled = 1")
    suspend fun getEnabledForContest(contestId: String): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1")
    suspend fun getAllEnabled(): List<ReminderEntity>

    @Upsert
    suspend fun upsert(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun delete(reminderId: String)

    @Query("DELETE FROM reminders WHERE contestId = :contestId")
    suspend fun deleteForContest(contestId: String)
}
