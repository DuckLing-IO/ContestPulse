package io.duckling.contestpulse.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.duckling.contestpulse.core.database.entity.FavoriteEntity

@Dao
interface FavoriteDao {
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE contestId = :contestId)")
    suspend fun isFavorite(contestId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE contestId = :contestId")
    suspend fun delete(contestId: String)
}
