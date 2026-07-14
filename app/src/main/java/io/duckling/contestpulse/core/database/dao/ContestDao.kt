package io.duckling.contestpulse.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.duckling.contestpulse.core.database.entity.ContestEntity
import io.duckling.contestpulse.core.database.model.ContestWithFavorite
import kotlinx.coroutines.flow.Flow

@Dao
interface ContestDao {
    @Transaction
    @Query("SELECT * FROM contests ORDER BY startTimeEpochMillis ASC")
    fun observeAllWithFavorite(): Flow<List<ContestWithFavorite>>

    @Transaction
    @Query("SELECT * FROM contests WHERE id = :contestId LIMIT 1")
    fun observeByIdWithFavorite(contestId: String): Flow<ContestWithFavorite?>

    @Query("SELECT * FROM contests WHERE id = :contestId LIMIT 1")
    suspend fun getById(contestId: String): ContestEntity?

    @Query("DELETE FROM contests WHERE id = :contestId")
    suspend fun deleteById(contestId: String)

    @Upsert
    suspend fun upsertAll(contests: List<ContestEntity>)

    @Query(
        """
        DELETE FROM contests
        WHERE source = :source
          AND lastUpdatedAtEpochMillis < :refreshEpochMillis
          AND id NOT IN (SELECT contestId FROM favorites)
        """,
    )
    suspend fun deleteStaleUnfavoritedForSource(
        source: String,
        refreshEpochMillis: Long,
    ): Int

    @Query(
        """
        DELETE FROM contests
        WHERE source = 'OTHER'
          AND sourceContestId LIKE :sourceContestIdPrefix || '%'
          AND lastUpdatedAtEpochMillis < :refreshEpochMillis
          AND id NOT IN (SELECT contestId FROM favorites)
        """,
    )
    suspend fun deleteStaleUnfavoritedForCustomSource(
        sourceContestIdPrefix: String,
        refreshEpochMillis: Long,
    ): Int

    @Query(
        """
        SELECT id FROM contests
        WHERE source = 'OTHER' AND sourceContestId LIKE :sourceContestIdPrefix || '%'
        """,
    )
    suspend fun getIdsForCustomSource(sourceContestIdPrefix: String): List<String>

    @Query(
        """
        DELETE FROM contests
        WHERE source = 'OTHER' AND sourceContestId LIKE :sourceContestIdPrefix || '%'
        """,
    )
    suspend fun deleteForCustomSource(sourceContestIdPrefix: String): Int

    @Query(
        """
        DELETE FROM contests
        WHERE endTimeEpochMillis IS NOT NULL
          AND endTimeEpochMillis < :cutoffEpochMillis
          AND id NOT IN (SELECT contestId FROM favorites)
        """,
    )
    suspend fun deleteExpiredUnfavorited(cutoffEpochMillis: Long): Int
}
