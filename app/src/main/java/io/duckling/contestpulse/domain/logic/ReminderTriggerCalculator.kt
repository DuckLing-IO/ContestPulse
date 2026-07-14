package io.duckling.contestpulse.domain.logic

import java.time.DateTimeException
import java.time.Duration
import java.time.Instant

fun calculateReminderTrigger(
    contestStart: Instant,
    offset: Duration,
    now: Instant,
): Instant? {
    require(!offset.isNegative) { "Reminder offset must not be negative" }
    val trigger = try {
        contestStart.minus(offset)
    } catch (_: DateTimeException) {
        return null
    } catch (_: ArithmeticException) {
        return null
    }
    return trigger.takeIf { it.isAfter(now) }
}

fun stableReminderRequestCode(reminderId: String): Int {
    require(reminderId.isNotBlank()) { "Reminder id must not be blank" }
    return reminderId.hashCode() and Int.MAX_VALUE
}
