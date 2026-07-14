package io.duckling.contestpulse.domain.model

import java.time.Instant

enum class SyncErrorType {
    NETWORK,
    HTTP,
    RATE_LIMIT,
    PARSING,
    REMOTE_API,
    UNKNOWN,
}

data class SyncIssue(
    val type: SyncErrorType,
    val diagnosticMessage: String?,
)

data class SourceSyncStatus(
    val source: ContestSource,
    val sourceKey: String = source.name,
    val lastAttemptAt: Instant,
    val lastSuccessAt: Instant?,
    val fetchedCount: Int,
    val issue: SyncIssue?,
)

data class ContestSyncState(
    val isRefreshing: Boolean = false,
    val sources: List<SourceSyncStatus> = emptyList(),
)

data class SourceSyncResult(
    val source: ContestSource,
    val sourceKey: String = source.name,
    val fetchedCount: Int,
    val issue: SyncIssue?,
)

data class SyncReport(
    val results: List<SourceSyncResult>,
    val alreadyRunning: Boolean = false,
)
