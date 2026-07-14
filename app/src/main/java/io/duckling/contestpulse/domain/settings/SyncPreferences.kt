package io.duckling.contestpulse.domain.settings

import io.duckling.contestpulse.domain.model.ContestSource

data class SyncPreferences(
    val backgroundSyncEnabled: Boolean = true,
    val wifiOnly: Boolean = false,
    val intervalHours: Int = DEFAULT_SYNC_INTERVAL_HOURS,
    val enabledSources: Set<ContestSource> = DEFAULT_ENABLED_SOURCES,
) {
    init {
        require(intervalHours in SUPPORTED_SYNC_INTERVALS) {
            "Unsupported sync interval: $intervalHours"
        }
    }
}

val SUPPORTED_SYNC_INTERVALS = setOf(6, 12, 24)
const val DEFAULT_SYNC_INTERVAL_HOURS = 12
val DEFAULT_ENABLED_SOURCES = setOf(
    ContestSource.CODEFORCES,
    ContestSource.ATCODER,
    ContestSource.LUOGU,
    ContestSource.NOWCODER,
)
