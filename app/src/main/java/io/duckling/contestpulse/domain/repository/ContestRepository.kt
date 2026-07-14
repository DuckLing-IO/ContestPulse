package io.duckling.contestpulse.domain.repository

import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestSyncState
import io.duckling.contestpulse.domain.model.SyncReport
import kotlinx.coroutines.flow.Flow
import java.time.Duration

interface ContestRepository {
    fun observeContests(): Flow<List<Contest>>

    fun observeContest(contestId: String): Flow<Contest?>

    fun observeSyncState(): Flow<ContestSyncState>

    suspend fun toggleFavorite(contestId: String): Result<Unit>

    suspend fun refresh(): SyncReport

    suspend fun refreshIfStale(maxAge: Duration): SyncReport

    suspend fun deleteCustomSourceData(sourceKey: String)
}
