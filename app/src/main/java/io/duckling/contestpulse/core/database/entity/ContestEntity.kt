package io.duckling.contestpulse.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contests",
    indices = [
        Index(value = ["source", "sourceContestId"], unique = true),
        Index(value = ["startTimeEpochMillis"]),
        Index(value = ["source"]),
    ],
)
data class ContestEntity(
    @PrimaryKey val id: String,
    val source: String,
    val sourceContestId: String,
    val title: String,
    val startTimeEpochMillis: Long,
    val endTimeEpochMillis: Long?,
    val durationMinutes: Long?,
    val registrationUrl: String?,
    val contestUrl: String,
    val status: String,
    val category: String?,
    val difficultyLabel: String?,
    val ratedRange: String?,
    val isRated: Boolean?,
    val lastUpdatedAtEpochMillis: Long,
    val remoteFingerprint: String,
)
