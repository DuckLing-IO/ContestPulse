package io.duckling.contestpulse.domain

import io.duckling.contestpulse.domain.logic.countdownAt
import io.duckling.contestpulse.domain.model.ContestCountdown
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ContestCountdownCalculatorTest {
    private val contest = testContest(
        startTime = Instant.parse("2026-07-16T12:00:00Z"),
    )

    @Test
    fun moreThanOneDay_returnsWholeDays() {
        assertEquals(
            ContestCountdown.Days(2),
            contest.countdownAt(Instant.parse("2026-07-14T12:00:00Z")),
        )
    }

    @Test
    fun lessThanOneDay_roundsUpToMinute() {
        assertEquals(
            ContestCountdown.HoursMinutes(hours = 1, minutes = 1),
            contest.countdownAt(Instant.parse("2026-07-16T10:59:01Z")),
        )
    }

    @Test
    fun afterStart_returnsRunning() {
        assertEquals(
            ContestCountdown.Running,
            contest.countdownAt(Instant.parse("2026-07-16T12:30:00Z")),
        )
    }
}
