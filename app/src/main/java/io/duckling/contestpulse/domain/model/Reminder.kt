package io.duckling.contestpulse.domain.model

import java.time.Instant

object ReminderProductConfig {
    const val MIN_DAY_OFFSET = 0
    const val MAX_DAY_OFFSET = 30
    const val MIN_HOUR = 0
    const val MAX_HOUR = 23
    const val MIN_MINUTE = 0
    const val MAX_MINUTE = 59
    const val MINUTE_STEP = 1
    const val MINUTES_PER_HOUR = 60
    const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
    const val MAX_RELATIVE_OFFSET_MINUTES =
        MAX_DAY_OFFSET * MINUTES_PER_DAY + MAX_HOUR * MINUTES_PER_HOUR + MAX_MINUTE
}

sealed interface ReminderRule {
    data class Relative(val offsetMinutes: Int) : ReminderRule {
        init {
            require(offsetMinutes >= 0) { "Reminder offset must not be negative" }
        }
    }

    data class FixedTime(
        val dayOffset: Int,
        val hour: Int,
        val minute: Int,
    ) : ReminderRule
}

data class ReminderDefinition(
    val id: String,
    val rule: ReminderRule,
    val createdAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "Reminder id must not be blank" }
    }
}

enum class ReminderMode {
    DEFAULT_SNAPSHOT,
    CUSTOM,
}

enum class ReminderScheduleStatus {
    SCHEDULED_EXACT,
    SCHEDULED_INEXACT,
    UNSCHEDULED,
    EXPIRED,
    INVALID,
    FIRED,
}

enum class ReminderDeliveryStatus {
    NOT_ATTEMPTED,
    DELIVERED,
    DELIVERY_FAILED,
}

enum class ReminderFailureReason {
    NONE,
    NOTIFICATION_PERMISSION,
    EXACT_ALARM_UNAVAILABLE,
    SYSTEM_SCHEDULING_FAILURE,
    DELIVERY_FAILURE,
}

data class ScheduledReminder(
    val definition: ReminderDefinition,
    val triggerAt: Instant?,
    val scheduleStatus: ReminderScheduleStatus,
    val deliveryStatus: ReminderDeliveryStatus,
    val failureReason: ReminderFailureReason = ReminderFailureReason.NONE,
) {
    val id: String get() = definition.id
    val rule: ReminderRule get() = definition.rule
}

fun ReminderRule.ruleKey(): String = when (this) {
    is ReminderRule.Relative -> "relative:$offsetMinutes"
    is ReminderRule.FixedTime -> "fixed:$dayOffset:$hour:$minute"
}
