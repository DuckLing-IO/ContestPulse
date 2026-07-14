package io.duckling.contestpulse.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = ContestEntity::class,
            parentColumns = ["id"],
            childColumns = ["contestId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["contestId"]),
        Index(value = ["contestId", "offsetMinutes"], unique = true),
        Index(value = ["triggerAtEpochMillis"]),
    ],
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val contestId: String,
    val triggerAtEpochMillis: Long,
    val offsetMinutes: Long,
    val isEnabled: Boolean,
    val schedulerType: String,
    val systemRequestCode: Int,
    val createdAtEpochMillis: Long,
)
