package io.duckling.contestpulse.domain.settings

import io.duckling.contestpulse.domain.model.ContestSource

data class SyncPreferences(
    val backgroundSyncEnabled: Boolean = true,
    val wifiOnly: Boolean = false,
    val intervalHours: Int = DEFAULT_SYNC_INTERVAL_HOURS,
    val enabledSources: Set<ContestSource> = DEFAULT_ENABLED_SOURCES,
    val defaultReminderOffsetMinutes: Int = DEFAULT_REMINDER_OFFSET_MINUTES,
) {
    init {
        require(intervalHours in SUPPORTED_SYNC_INTERVALS) {
            "Unsupported sync interval: $intervalHours"
        }
        require(defaultReminderOffsetMinutes in SUPPORTED_REMINDER_OFFSET_MINUTES) {
            "Unsupported default reminder offset: $defaultReminderOffsetMinutes"
        }
    }
}

val SUPPORTED_SYNC_INTERVALS = setOf(6, 12, 24)
const val DEFAULT_SYNC_INTERVAL_HOURS = 12
const val DEFAULT_REMINDER_OFFSET_MINUTES = 60
val SUPPORTED_REMINDER_OFFSET_MINUTES = setOf(1_440, 180, 60, 15, 0)
val DEFAULT_ENABLED_SOURCES = setOf(
    ContestSource.CODEFORCES,
    ContestSource.ATCODER,
    ContestSource.LUOGU,
    ContestSource.NOWCODER,
)
