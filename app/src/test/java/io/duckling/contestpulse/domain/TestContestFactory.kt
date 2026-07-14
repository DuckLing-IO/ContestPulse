package io.duckling.contestpulse.domain

import io.duckling.contestpulse.domain.logic.stableContestId
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.model.ContestStatus
import java.time.Duration
import java.time.Instant

fun testContest(
    sourceContestId: String = "test-1",
    source: ContestSource = ContestSource.CODEFORCES,
    title: String = "Test Contest",
    startTime: Instant = Instant.parse("2026-07-14T12:00:00Z"),
    duration: Duration? = Duration.ofHours(2),
    endTime: Instant? = duration?.let(startTime::plus),
    status: ContestStatus = ContestStatus.UPCOMING,
    isRated: Boolean? = true,
    isFavorite: Boolean = false,
): Contest = Contest(
    id = stableContestId(source, sourceContestId),
    source = source,
    sourceContestId = sourceContestId,
    title = title,
    startTime = startTime,
    endTime = endTime,
    duration = duration,
    registrationUrl = null,
    contestUrl = "https://example.com/contest/$sourceContestId",
    status = status,
    category = "Demo",
    difficultyLabel = null,
    ratedRange = if (isRated == true) "0 - 1999" else null,
    isRated = isRated,
    isFavorite = isFavorite,
    reminderOffsets = emptySet(),
    lastUpdatedAt = Instant.parse("2026-07-14T00:00:00Z"),
)
