package io.duckling.contestpulse.domain

import io.duckling.contestpulse.domain.logic.ReminderTriggerResult
import io.duckling.contestpulse.domain.logic.ReminderValidationError
import io.duckling.contestpulse.domain.logic.calculateReminderTrigger
import io.duckling.contestpulse.domain.logic.deriveReminderTrigger
import io.duckling.contestpulse.domain.logic.DerivedReminderTrigger
import io.duckling.contestpulse.domain.logic.formatReminder
import io.duckling.contestpulse.domain.logic.hasDuplicateReminder
import io.duckling.contestpulse.domain.model.ReminderRule
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderRuleTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = Instant.parse("2026-07-16T00:00:00Z")
    private val start = Instant.parse("2026-07-18T12:00:00Z")

    @Test
    fun relativeRuleSubtractsExactDuration() {
        assertEquals(
            ReminderTriggerResult.Valid(Instant.parse("2026-07-16T12:00:00Z")),
            calculateReminderTrigger(start, ReminderRule.Relative(2 * 24 * 60), now, zone),
        )
    }

    @Test
    fun zeroRelativeRuleIsInvalidButRepresentable() {
        assertEquals(
            ReminderTriggerResult.Invalid(ReminderValidationError.ZERO_OFFSET),
            calculateReminderTrigger(start, ReminderRule.Relative(0), now, zone),
        )
    }

    @Test
    fun fixedTimeUsesContestLocalDate() {
        assertEquals(
            ReminderTriggerResult.Valid(Instant.parse("2026-07-17T12:30:00Z")),
            calculateReminderTrigger(start, ReminderRule.FixedTime(1, 20, 30), now, zone),
        )
    }

    @Test
    fun sameDayFixedTimeMustBeStrictlyBeforeStart() {
        assertEquals(
            ReminderTriggerResult.Invalid(ReminderValidationError.NOT_BEFORE_CONTEST),
            calculateReminderTrigger(start, ReminderRule.FixedTime(0, 20, 0), now, zone),
        )
    }

    @Test
    fun sameDayFixedTimeEarlierThanStartIsAccepted() {
        assertEquals(
            ReminderTriggerResult.Valid(Instant.parse("2026-07-18T10:30:00Z")),
            calculateReminderTrigger(start, ReminderRule.FixedTime(0, 18, 30), now, zone),
        )
    }

    @Test
    fun relativeHourAndMinuteOffsetsArePreservedForLegacyEditing() {
        assertEquals(
            ReminderTriggerResult.Valid(Instant.parse("2026-07-18T10:40:00Z")),
            calculateReminderTrigger(start, ReminderRule.Relative(80), now, zone),
        )
    }

    @Test
    fun fixedTimeWorksAcrossYearBoundary() {
        val newYearContest = Instant.parse("2027-01-01T12:00:00Z")
        assertEquals(
            ReminderTriggerResult.Valid(Instant.parse("2026-12-31T12:00:00Z")),
            calculateReminderTrigger(
                newYearContest,
                ReminderRule.FixedTime(1, 20, 0),
                now,
                zone,
            ),
        )
    }

    @Test
    fun nonexistentDstLocalTimeIsInvalid() {
        val newYork = ZoneId.of("America/New_York")
        val contestAfterGap = Instant.parse("2026-03-08T16:00:00Z")
        assertEquals(
            DerivedReminderTrigger.Invalid(ReminderValidationError.NONEXISTENT_LOCAL_TIME),
            deriveReminderTrigger(
                contestAfterGap,
                ReminderRule.FixedTime(0, 2, 30),
                newYork,
            ),
        )
    }

    @Test
    fun dstOverlapSelectsEarlierInstantDeterministically() {
        val newYork = ZoneId.of("America/New_York")
        val contestAfterOverlap = Instant.parse("2026-11-01T17:00:00Z")
        assertEquals(
            DerivedReminderTrigger.Valid(Instant.parse("2026-11-01T05:30:00Z")),
            deriveReminderTrigger(
                contestAfterOverlap,
                ReminderRule.FixedTime(0, 1, 30),
                newYork,
            ),
        )
    }

    @Test
    fun duplicateTriggerIsRejectedEvenForDifferentRules() {
        assertTrue(
            hasDuplicateReminder(
                candidate = ReminderRule.FixedTime(0, 19, 0),
                existing = listOf(ReminderRule.Relative(60)),
                contestStart = start,
                now = now,
                zoneId = zone,
            ),
        )
    }

    @Test
    fun formatterUsesUnifiedLabels() {
        assertEquals("比赛前 2 天", formatReminder(ReminderRule.Relative(2 * 24 * 60)))
        assertEquals("比赛前 2 小时 30 分钟", formatReminder(ReminderRule.Relative(150)))
        assertEquals("比赛当天 18:30", formatReminder(ReminderRule.FixedTime(0, 18, 30)))
        assertEquals("比赛前 1 天 09:05", formatReminder(ReminderRule.FixedTime(1, 9, 5)))
    }
}
