package io.duckling.contestpulse.data.repository

import androidx.room.withTransaction
import io.duckling.contestpulse.core.database.ContestPulseDatabase
import io.duckling.contestpulse.core.database.dao.ContestDao
import io.duckling.contestpulse.core.database.dao.FavoriteDao
import io.duckling.contestpulse.core.database.dao.SyncStatusDao
import io.duckling.contestpulse.core.database.entity.FavoriteEntity
import io.duckling.contestpulse.core.database.entity.SyncStatusEntity
import io.duckling.contestpulse.core.database.toDomain
import io.duckling.contestpulse.core.database.toEntity
import io.duckling.contestpulse.data.remote.ContestSourceFetchOutcome
import io.duckling.contestpulse.data.remote.RemoteContestFetcher
import io.duckling.contestpulse.data.remote.toSyncIssue
import io.duckling.contestpulse.domain.customsource.CUSTOM_SOURCE_KEY_PREFIX
import io.duckling.contestpulse.domain.customsource.CustomSourceRepository
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.model.ContestSyncState
import io.duckling.contestpulse.domain.model.SourceSyncResult
import io.duckling.contestpulse.domain.model.SourceSyncStatus
import io.duckling.contestpulse.domain.model.SyncErrorType
import io.duckling.contestpulse.domain.model.SyncIssue
import io.duckling.contestpulse.domain.model.SyncReport
import io.duckling.contestpulse.domain.repository.ContestRepository
import io.duckling.contestpulse.domain.reminder.ReminderManager
import io.duckling.contestpulse.domain.settings.SettingsRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex

@Singleton
class OfflineFirstContestRepository @Inject constructor(
    private val database: ContestPulseDatabase,
    private val contestDao: ContestDao,
    private val favoriteDao: FavoriteDao,
    private val syncStatusDao: SyncStatusDao,
    private val remoteContestFetcher: RemoteContestFetcher,
    private val reminderManager: ReminderManager,
    private val settingsRepository: SettingsRepository,
    private val customSourceRepository: CustomSourceRepository,
    private val clock: Clock,
) : ContestRepository {
    private val refreshMutex = Mutex()
    private val isRefreshing = MutableStateFlow(false)

    override fun observeContests(): Flow<List<Contest>> =
        contestDao.observeAllWithFavorite().map { rows -> rows.map { it.toDomain() } }

    override fun observeContest(contestId: String): Flow<Contest?> =
        contestDao.observeByIdWithFavorite(contestId).map { row -> row?.toDomain() }

    override fun observeSyncState(): Flow<ContestSyncState> = combine(
        syncStatusDao.observeAll(),
        isRefreshing,
    ) { statuses, refreshing ->
        ContestSyncState(
            isRefreshing = refreshing,
            sources = statuses.map(SyncStatusEntity::toDomain),
        )
    }

    override suspend fun toggleFavorite(contestId: String): Result<Unit> = runCatching {
        val removedFavorite = database.withTransaction {
            check(contestDao.getById(contestId) != null) { "Contest not found: $contestId" }
            if (favoriteDao.isFavorite(contestId)) {
                favoriteDao.delete(contestId)
                true
            } else {
                favoriteDao.insert(
                    FavoriteEntity(
                        contestId = contestId,
                        createdAtEpochMillis = clock.millis(),
                        note = null,
                    ),
                )
                false
            }
        }
        if (removedFavorite) reminderManager.clearForContest(contestId)
    }

    override suspend fun refresh(): SyncReport {
        if (!refreshMutex.tryLock()) {
            return SyncReport(results = emptyList(), alreadyRunning = true)
        }
        isRefreshing.value = true
        return try {
            val enabledSources = settingsRepository.preferences.first().enabledSources
            val outcomes = remoteContestFetcher.fetchAll(enabledSources)
            val results = outcomes.map { outcome ->
                when (outcome) {
                    is ContestSourceFetchOutcome.Success -> persistSuccess(outcome)
                    is ContestSourceFetchOutcome.Failure -> persistFailure(outcome)
                }
            }
            contestDao.deleteExpiredUnfavorited(
                cutoffEpochMillis = clock.instant().minus(CACHE_RETENTION).toEpochMilli(),
            )
            SyncReport(results = results)
        } finally {
            isRefreshing.value = false
            refreshMutex.unlock()
        }
    }

    override suspend fun refreshIfStale(maxAge: Duration): SyncReport {
        require(!maxAge.isNegative) { "Maximum cache age must not be negative" }
        val enabledSources = settingsRepository.preferences.first().enabledSources
        val enabledCustomSources = customSourceRepository.sources.first()
            .filter { source -> source.enabled }
        val expectedSourceKeys = enabledSources.mapTo(mutableSetOf(), ContestSource::name) +
            enabledCustomSources.map { source -> source.sourceKey }
        if (expectedSourceKeys.isEmpty()) return SyncReport(results = emptyList())
        val cutoff = clock.instant().minus(maxAge).toEpochMilli()
        val currentStatuses = syncStatusDao.getAll().associateBy(SyncStatusEntity::source)
        val isFresh = expectedSourceKeys.all { sourceKey ->
            val status = currentStatuses[sourceKey]
            status?.lastSuccessAtEpochMillis != null &&
                status.lastSuccessAtEpochMillis >= cutoff &&
                status.lastErrorType == null
        }
        return if (isFresh) SyncReport(results = emptyList()) else refresh()
    }

    private suspend fun persistSuccess(
        outcome: ContestSourceFetchOutcome.Success,
    ): SourceSyncResult {
        val syncedAt = clock.instant()
        database.withTransaction {
            contestDao.upsertAll(
                outcome.contests.map { contest ->
                    contest.copy(lastUpdatedAt = syncedAt).toEntity()
                },
            )
            if (outcome.contests.isNotEmpty()) {
                if (outcome.sourceKey.startsWith(CUSTOM_SOURCE_KEY_PREFIX)) {
                    contestDao.deleteStaleUnfavoritedForCustomSource(
                        sourceContestIdPrefix = outcome.sourceKey.removePrefix(
                            CUSTOM_SOURCE_KEY_PREFIX,
                        ) + ":",
                        refreshEpochMillis = syncedAt.toEpochMilli(),
                    )
                } else {
                    contestDao.deleteStaleUnfavoritedForSource(
                        source = outcome.source.name,
                        refreshEpochMillis = syncedAt.toEpochMilli(),
                    )
                }
            }
            syncStatusDao.upsert(
                SyncStatusEntity(
                    source = outcome.sourceKey,
                    lastAttemptAtEpochMillis = syncedAt.toEpochMilli(),
                    lastSuccessAtEpochMillis = syncedAt.toEpochMilli(),
                    lastErrorType = null,
                    lastErrorMessage = null,
                    fetchedCount = outcome.contests.size,
                ),
            )
        }
        try {
            reminderManager.rescheduleForContests(outcome.contests)
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // The cached contest sync remains valid; boot/app-open rescheduling will retry later.
        }
        return SourceSyncResult(
            source = outcome.source,
            sourceKey = outcome.sourceKey,
            fetchedCount = outcome.contests.size,
            issue = null,
        )
    }

    private suspend fun persistFailure(
        outcome: ContestSourceFetchOutcome.Failure,
    ): SourceSyncResult {
        val attemptedAt = clock.instant()
        val previous = syncStatusDao.get(outcome.sourceKey)
        val issue = outcome.throwable.toSyncIssue()
        syncStatusDao.upsert(
            SyncStatusEntity(
                source = outcome.sourceKey,
                lastAttemptAtEpochMillis = attemptedAt.toEpochMilli(),
                lastSuccessAtEpochMillis = previous?.lastSuccessAtEpochMillis,
                lastErrorType = issue.type.name,
                lastErrorMessage = issue.diagnosticMessage,
                fetchedCount = previous?.fetchedCount ?: 0,
            ),
        )
        return SourceSyncResult(
            source = outcome.source,
            sourceKey = outcome.sourceKey,
            fetchedCount = 0,
            issue = issue,
        )
    }

    override suspend fun deleteCustomSourceData(sourceKey: String) {
        require(sourceKey.startsWith(CUSTOM_SOURCE_KEY_PREFIX)) { "Not a custom source key" }
        val prefix = sourceKey.removePrefix(CUSTOM_SOURCE_KEY_PREFIX) + ":"
        val contestIds = contestDao.getIdsForCustomSource(prefix)
        contestIds.forEach { contestId -> reminderManager.clearForContest(contestId) }
        database.withTransaction {
            contestDao.deleteForCustomSource(prefix)
            syncStatusDao.delete(sourceKey)
        }
    }
}

private fun SyncStatusEntity.toDomain(): SourceSyncStatus = SourceSyncStatus(
    source = if (source.startsWith(CUSTOM_SOURCE_KEY_PREFIX)) {
        ContestSource.OTHER
    } else {
        enumValueOrDefault(source, ContestSource.OTHER)
    },
    sourceKey = source,
    lastAttemptAt = Instant.ofEpochMilli(lastAttemptAtEpochMillis),
    lastSuccessAt = lastSuccessAtEpochMillis?.let(Instant::ofEpochMilli),
    fetchedCount = fetchedCount,
    issue = lastErrorType?.let { type ->
        SyncIssue(
            type = enumValueOrDefault(type, SyncErrorType.UNKNOWN),
            diagnosticMessage = lastErrorMessage,
        )
    },
)

private inline fun <reified T : Enum<T>> enumValueOrDefault(
    value: String,
    default: T,
): T = enumValues<T>().firstOrNull { it.name == value } ?: default

private val CACHE_RETENTION: Duration = Duration.ofDays(30)
