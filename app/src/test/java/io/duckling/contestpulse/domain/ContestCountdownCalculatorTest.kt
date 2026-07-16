package io.duckling.contestpulse.domain

import io.duckling.contestpulse.domain.logic.countdownAt
import io.duckling.contestpulse.domain.model.ContestCountdown
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ContestCountdownCalculatorTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")
    private val contest = testContest(
        startTime = Instant.parse("2026-07-16T12:00:00Z"),
    )

    @Test
    fun moreThanOneDay_returnsWholeDays() {
        assertEquals(
            ContestCountdown.Days(2),
            contest.countdownAt(Instant.parse("2026-07-14T12:00:00Z"), zoneId),
        )
    }

    @Test
    fun lessThanOneDay_roundsUpToMinute() {
        assertEquals(
            ContestCountdown.HoursMinutes(hours = 1, minutes = 1),
            contest.countdownAt(Instant.parse("2026-07-16T10:59:01Z"), zoneId),
        )
    }

    @Test
    fun afterStart_returnsRunning() {
        assertEquals(
            ContestCountdown.Running,
            contest.countdownAt(Instant.parse("2026-07-16T12:30:00Z"), zoneId),
        )
    }

    @Test
    fun daysUseLocalCalendarDatesInsteadOfWholeDurations() {
        val now = Instant.parse("2026-07-16T12:00:00Z")
        val july18 = testContest(startTime = Instant.parse("2026-07-18T12:00:00Z"))
        val july19Early = testContest(startTime = Instant.parse("2026-07-19T11:00:00Z"))

        assertEquals(ContestCountdown.Days(2), july18.countdownAt(now, zoneId))
        assertEquals(ContestCountdown.Days(3), july19Early.countdownAt(now, zoneId))
    }

    @Test
    fun sameLocalDateUsesHoursAndMinutes() {
        val contest = testContest(startTime = Instant.parse("2026-07-16T15:00:00Z"))

        assertEquals(
            ContestCountdown.HoursMinutes(3, 0),
            contest.countdownAt(Instant.parse("2026-07-16T12:00:00Z"), zoneId),
        )
    }

    @Test
    fun localCalendarDifferenceHandlesMonthAndYearBoundaries() {
        val december31 = Instant.parse("2026-12-31T04:00:00Z")
        val january2 = testContest(startTime = Instant.parse("2027-01-02T01:00:00Z"))

        assertEquals(ContestCountdown.Days(2), january2.countdownAt(december31, zoneId))
    }

    @Test
    fun sameInstantsUseTheSuppliedTimeZone() {
        val now = Instant.parse("2026-07-16T15:30:00Z")
        val target = testContest(startTime = Instant.parse("2026-07-16T17:00:00Z"))

        assertEquals(
            ContestCountdown.Days(1),
            target.countdownAt(now, ZoneId.of("Asia/Shanghai")),
        )
        assertEquals(
            ContestCountdown.HoursMinutes(1, 30),
            target.countdownAt(now, ZoneId.of("America/New_York")),
        )
    }

    @Test
    fun reorderedAndRemovedItemsKeepIndependentCountdowns() {
        val now = Instant.parse("2026-07-16T04:00:00Z")
        val contests = listOf(
            testContest(startTime = Instant.parse("2026-07-18T12:00:00Z")),
            testContest(startTime = Instant.parse("2026-07-19T11:00:00Z")),
        )

        val reordered = contests.reversed().map { it.countdownAt(now, zoneId) }
        val afterRemovingFirst = contests.drop(1).map { it.countdownAt(now, zoneId) }

        assertEquals(listOf(ContestCountdown.Days(3), ContestCountdown.Days(2)), reordered)
        assertEquals(listOf(ContestCountdown.Days(3)), afterRemovingFirst)
    }
}
