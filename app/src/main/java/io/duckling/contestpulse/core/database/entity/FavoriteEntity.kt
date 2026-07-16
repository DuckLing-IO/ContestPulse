package io.duckling.contestpulse.core.database.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
    foreignKeys = [
        ForeignKey(
            entity = ContestEntity::class,
            parentColumns = ["id"],
            childColumns = ["contestId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["contestId"], unique = true)],
)
data class FavoriteEntity(
    @PrimaryKey val contestId: String,
    val createdAtEpochMillis: Long,
    val note: String?,
    @ColumnInfo(defaultValue = "'CUSTOM'")
    val reminderMode: String = "CUSTOM",
)
