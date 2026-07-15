package io.duckling.contestpulse.domain.settings

import io.duckling.contestpulse.domain.model.ContestSource

data class SyncPreferences(
    val backgroundSyncEnabled: Boolean = true,
    val wifiOnly: Boolean = false,
    val intervalHours: Int = DEFAULT_SYNC_INTERVAL_HOURS,
    val enabledSources: Set<ContestSource> = DEFAULT_ENABLED_SOURCES,
    val defaultReminderOffsetsMinutes: Set<Int> = DEFAULT_REMINDER_OFFSETS_MINUTES,
) {
    init {
        require(intervalHours in SUPPORTED_SYNC_INTERVALS) {
            "Unsupported sync interval: $intervalHours"
        }
        require(defaultReminderOffsetsMinutes.all { it in MIN_REMINDER_OFFSET_MINUTES..MAX_REMINDER_OFFSET_MINUTES }) {
            "Unsupported default reminder offsets: $defaultReminderOffsetsMinutes"
        }
    }
}

val SUPPORTED_SYNC_INTERVALS = setOf(6, 12, 24)
const val DEFAULT_SYNC_INTERVAL_HOURS = 12
const val DEFAULT_REMINDER_OFFSET_MINUTES = 60
val DEFAULT_REMINDER_OFFSETS_MINUTES = setOf(DEFAULT_REMINDER_OFFSET_MINUTES)
const val MIN_REMINDER_OFFSET_MINUTES = 0
const val MAX_REMINDER_OFFSET_MINUTES = 30 * 24 * 60 + 23 * 60 + 59
val DEFAULT_ENABLED_SOURCES = setOf(
    ContestSource.CODEFORCES,
    ContestSource.ATCODER,
    ContestSource.LUOGU,
    ContestSource.NOWCODER,
)
