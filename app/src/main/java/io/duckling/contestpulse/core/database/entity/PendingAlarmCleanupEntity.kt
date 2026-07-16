package io.duckling.contestpulse.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_alarm_cleanup")
data class PendingAlarmCleanupEntity(
    @PrimaryKey val id: String,
    val reminderId: String,
    val requestCode: Int,
    val pendingIntentVersion: Int,
    val createdAtEpochMillis: Long,
)
