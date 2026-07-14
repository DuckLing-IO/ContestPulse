package io.duckling.contestpulse.domain

import io.duckling.contestpulse.domain.logic.statusAt
import io.duckling.contestpulse.domain.model.ContestStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ContestStatusResolverTest {
    private val contest = testContest(
        startTime = Instant.parse("2026-07-14T12:00:00Z"),
    )

    @Test
    fun beforeStart_isUpcoming() {
        assertEquals(
            ContestStatus.UPCOMING,
            contest.statusAt(Instant.parse("2026-07-14T11:59:59Z")),
        )
    }

    @Test
    fun betweenStartAndEnd_isRunning() {
        assertEquals(
            ContestStatus.RUNNING,
            contest.statusAt(Instant.parse("2026-07-14T13:00:00Z")),
        )
    }

    @Test
    fun atEnd_isFinished() {
        assertEquals(
            ContestStatus.FINISHED,
            contest.statusAt(Instant.parse("2026-07-14T14:00:00Z")),
        )
    }
}
