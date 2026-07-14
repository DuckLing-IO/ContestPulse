package io.duckling.contestpulse.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import io.duckling.contestpulse.core.database.entity.ContestEntity
import io.duckling.contestpulse.core.database.entity.FavoriteEntity
import io.duckling.contestpulse.core.database.entity.ReminderEntity

data class ContestWithFavorite(
    @Embedded val contest: ContestEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "contestId",
    )
    val favorite: FavoriteEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "contestId",
    )
    val reminders: List<ReminderEntity>,
)
