package io.duckling.contestpulse.domain

import io.duckling.contestpulse.domain.logic.buildContestTimeline
import io.duckling.contestpulse.domain.model.ContestGroupType
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ContestTimelineTest {
    private val now = Instant.parse("2026-07-14T02:00:00Z")
    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun groupsByUserLocalDateAndOmitsFinishedContests() {
        val contests = listOf(
            testContest(
                sourceContestId = "running",
                startTime = Instant.parse("2026-07-14T01:00:00Z"),
                duration = Duration.ofHours(2),
            ),
            testContest(
                sourceContestId = "today",
                startTime = Instant.parse("2026-07-14T07:00:00Z"),
            ),
            testContest(
                sourceContestId = "tomorrow",
                startTime = Instant.parse("2026-07-15T12:00:00Z"),
            ),
            testContest(
                sourceContestId = "week",
                startTime = Instant.parse("2026-07-17T12:00:00Z"),
            ),
            testContest(
                sourceContestId = "later",
                startTime = Instant.parse("2026-07-20T12:00:00Z"),
            ),
            testContest(
                sourceContestId = "finished",
                startTime = Instant.parse("2026-07-13T01:00:00Z"),
                duration = Duration.ofHours(1),
            ),
        )

        val result = buildContestTimeline(contests, now, zoneId)

        assertEquals(
            listOf(
                ContestGroupType.RUNNING,
                ContestGroupType.TODAY,
                ContestGroupType.TOMORROW,
                ContestGroupType.THIS_WEEK,
                ContestGroupType.LATER,
            ),
            result.map { it.type },
        )
        assertEquals(
            listOf("running", "today", "tomorrow", "week", "later"),
            result.flatMap { it.contests }.map { it.sourceContestId },
        )
    }
}
