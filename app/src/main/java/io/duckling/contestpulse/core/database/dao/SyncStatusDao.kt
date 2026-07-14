package io.duckling.contestpulse.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.duckling.contestpulse.core.database.entity.SyncStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncStatusDao {
    @Query("SELECT * FROM sync_status ORDER BY source ASC")
    fun observeAll(): Flow<List<SyncStatusEntity>>

    @Query("SELECT * FROM sync_status")
    suspend fun getAll(): List<SyncStatusEntity>

    @Query("SELECT * FROM sync_status WHERE source = :source LIMIT 1")
    suspend fun get(source: String): SyncStatusEntity?

    @Upsert
    suspend fun upsert(status: SyncStatusEntity)

    @Query("DELETE FROM sync_status WHERE source = :sourceKey")
    suspend fun delete(sourceKey: String)
}
