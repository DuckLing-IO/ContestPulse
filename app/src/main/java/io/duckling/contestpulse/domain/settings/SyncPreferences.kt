package io.duckling.contestpulse.domain.settings

import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.model.ReminderDefinition
import io.duckling.contestpulse.domain.model.ReminderProductConfig
import io.duckling.contestpulse.domain.model.ReminderRule
import java.time.Instant

data class SyncPreferences(
    val backgroundSyncEnabled: Boolean = true,
    val wifiOnly: Boolean = false,
    val intervalHours: Int = DEFAULT_SYNC_INTERVAL_HOURS,
    val enabledSources: Set<ContestSource> = DEFAULT_ENABLED_SOURCES,
    val defaultReminders: List<ReminderDefinition> = DEFAULT_REMINDERS,
) {
    val defaultReminderOffsetsMinutes: Set<Int>
        get() = defaultReminders.mapNotNull { reminder ->
            (reminder.rule as? ReminderRule.Relative)?.offsetMinutes
        }.toSet()

    init {
        require(intervalHours in SUPPORTED_SYNC_INTERVALS) {
            "Unsupported sync interval: $intervalHours"
        }
    }
}

val SUPPORTED_SYNC_INTERVALS = setOf(6, 12, 24)
const val DEFAULT_SYNC_INTERVAL_HOURS = 12
const val DEFAULT_REMINDER_OFFSET_MINUTES = 60
val DEFAULT_REMINDER_OFFSETS_MINUTES = setOf(DEFAULT_REMINDER_OFFSET_MINUTES)
const val MIN_REMINDER_OFFSET_MINUTES = 0
const val MAX_REMINDER_OFFSET_MINUTES = ReminderProductConfig.MAX_RELATIVE_OFFSET_MINUTES
val DEFAULT_REMINDERS = listOf(
    ReminderDefinition(
        id = "default-relative-$DEFAULT_REMINDER_OFFSET_MINUTES",
        rule = ReminderRule.Relative(DEFAULT_REMINDER_OFFSET_MINUTES),
        createdAt = Instant.EPOCH,
    ),
)
val DEFAULT_ENABLED_SOURCES = setOf(
    ContestSource.CODEFORCES,
    ContestSource.ATCODER,
    ContestSource.LUOGU,
    ContestSource.NOWCODER,
)
