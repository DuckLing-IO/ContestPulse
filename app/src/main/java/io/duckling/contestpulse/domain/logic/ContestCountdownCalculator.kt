package io.duckling.contestpulse.domain.logic

import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestCountdown
import io.duckling.contestpulse.domain.model.ContestStatus
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun Contest.countdownAt(now: Instant, zoneId: ZoneId): ContestCountdown = when (statusAt(now)) {
    ContestStatus.RUNNING -> ContestCountdown.Running
    ContestStatus.FINISHED -> ContestCountdown.Finished
    ContestStatus.UNKNOWN -> ContestCountdown.Unknown
    ContestStatus.UPCOMING -> {
        val calendarDays = ChronoUnit.DAYS.between(
            now.atZone(zoneId).toLocalDate(),
            startTime.atZone(zoneId).toLocalDate(),
        )
        val remaining = Duration.between(now, startTime)
        if (calendarDays > 0) {
            ContestCountdown.Days(calendarDays)
        } else {
            val totalMinutes = ((remaining.seconds.coerceAtLeast(0) + SECONDS_PER_MINUTE - 1) /
                SECONDS_PER_MINUTE)
            ContestCountdown.HoursMinutes(
                hours = totalMinutes / MINUTES_PER_HOUR,
                minutes = totalMinutes % MINUTES_PER_HOUR,
            )
        }
    }
}

private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L
