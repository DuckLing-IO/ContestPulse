package io.duckling.contestpulse.domain.logic

import io.duckling.contestpulse.domain.model.ReminderProductConfig
import io.duckling.contestpulse.domain.model.ReminderRule
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

enum class ReminderValidationError {
    ZERO_OFFSET,
    OUT_OF_RANGE,
    NOT_BEFORE_CONTEST,
    NOT_IN_FUTURE,
    NONEXISTENT_LOCAL_TIME,
    DUPLICATE,
}

sealed interface ReminderTriggerResult {
    data class Valid(val triggerAt: Instant) : ReminderTriggerResult
    data class Invalid(val error: ReminderValidationError) : ReminderTriggerResult
}

sealed interface DerivedReminderTrigger {
    data class Valid(val triggerAt: Instant) : DerivedReminderTrigger
    data class Invalid(val error: ReminderValidationError) : DerivedReminderTrigger
}

fun deriveReminderTrigger(
    contestStart: Instant,
    rule: ReminderRule,
    zoneId: ZoneId,
): DerivedReminderTrigger {
    validateReminderStructure(rule)?.let { return DerivedReminderTrigger.Invalid(it) }
    val trigger = when (rule) {
        is ReminderRule.Relative -> try {
            contestStart.minus(Duration.ofMinutes(rule.offsetMinutes.toLong()))
        } catch (_: DateTimeException) {
            return DerivedReminderTrigger.Invalid(ReminderValidationError.OUT_OF_RANGE)
        } catch (_: ArithmeticException) {
            return DerivedReminderTrigger.Invalid(ReminderValidationError.OUT_OF_RANGE)
        }
        is ReminderRule.FixedTime -> {
            val dateTime = contestStart.atZone(zoneId)
                .toLocalDate()
                .minusDays(rule.dayOffset.toLong())
                .atTime(rule.hour, rule.minute)
            resolveLocalDateTime(dateTime, zoneId)
                ?: return DerivedReminderTrigger.Invalid(
                    ReminderValidationError.NONEXISTENT_LOCAL_TIME,
                )
        }
    }
    return if (trigger.isBefore(contestStart)) {
        DerivedReminderTrigger.Valid(trigger)
    } else {
        DerivedReminderTrigger.Invalid(ReminderValidationError.NOT_BEFORE_CONTEST)
    }
}

fun validateReminderStructure(rule: ReminderRule): ReminderValidationError? = when (rule) {
    is ReminderRule.Relative -> when {
        rule.offsetMinutes == 0 -> ReminderValidationError.ZERO_OFFSET
        rule.offsetMinutes > ReminderProductConfig.MAX_RELATIVE_OFFSET_MINUTES ->
            ReminderValidationError.OUT_OF_RANGE
        else -> null
    }
    is ReminderRule.FixedTime -> when {
        rule.dayOffset !in ReminderProductConfig.MIN_DAY_OFFSET..ReminderProductConfig.MAX_DAY_OFFSET ->
            ReminderValidationError.OUT_OF_RANGE
        rule.hour !in ReminderProductConfig.MIN_HOUR..ReminderProductConfig.MAX_HOUR ->
            ReminderValidationError.OUT_OF_RANGE
        rule.minute !in ReminderProductConfig.MIN_MINUTE..ReminderProductConfig.MAX_MINUTE ->
            ReminderValidationError.OUT_OF_RANGE
        rule.minute % ReminderProductConfig.MINUTE_STEP != 0 -> ReminderValidationError.OUT_OF_RANGE
        else -> null
    }
}

fun calculateReminderTrigger(
    contestStart: Instant,
    rule: ReminderRule,
    now: Instant,
    zoneId: ZoneId,
): ReminderTriggerResult {
    return when (val derived = deriveReminderTrigger(contestStart, rule, zoneId)) {
        is DerivedReminderTrigger.Invalid -> ReminderTriggerResult.Invalid(derived.error)
        is DerivedReminderTrigger.Valid -> when {
        !derived.triggerAt.isAfter(now) -> ReminderTriggerResult.Invalid(ReminderValidationError.NOT_IN_FUTURE)
        else -> ReminderTriggerResult.Valid(derived.triggerAt)
        }
    }
}

fun hasDuplicateReminder(
    candidate: ReminderRule,
    existing: Collection<ReminderRule>,
    contestStart: Instant?,
    now: Instant,
    zoneId: ZoneId,
): Boolean {
    if (candidate in existing) return true
    if (contestStart == null) return false
    val candidateTrigger = calculateReminderTrigger(contestStart, candidate, now, zoneId)
        .let { it as? ReminderTriggerResult.Valid }
        ?.triggerAt
        ?: return false
    return existing.any { rule ->
        calculateReminderTrigger(contestStart, rule, now, zoneId)
            .let { it as? ReminderTriggerResult.Valid }
            ?.triggerAt == candidateTrigger
    }
}

private fun resolveLocalDateTime(dateTime: LocalDateTime, zoneId: ZoneId): Instant? {
    val offsets = zoneId.rules.getValidOffsets(dateTime)
    if (offsets.isEmpty()) return null
    return ZonedDateTime.ofLocal(dateTime, zoneId, offsets.first()).toInstant()
}
