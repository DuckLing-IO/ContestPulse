package io.duckling.contestpulse.domain.logic

import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestFilter
import io.duckling.contestpulse.domain.model.ContestDateRange
import java.time.Duration
import java.time.Instant

fun filterContests(
    contests: List<Contest>,
    filter: ContestFilter,
    now: Instant = Instant.EPOCH,
): List<Contest> {
    return contests.filter { contest ->
        val matchesSource = contest.source in filter.sources
        val matchesFavorite = !filter.favoriteOnly || contest.isFavorite
        val matchesRated = !filter.ratedOnly || contest.isRated == true
        val matchesDate = when (filter.dateRange) {
            ContestDateRange.ALL -> true
            ContestDateRange.NEXT_7_DAYS -> contest.isWithin(now, Duration.ofDays(7))
            ContestDateRange.NEXT_30_DAYS -> contest.isWithin(now, Duration.ofDays(30))
        }

        matchesSource && matchesFavorite && matchesRated && matchesDate
    }
}

private fun Contest.isWithin(
    now: Instant,
    window: Duration,
): Boolean = !startTime.isBefore(now) && startTime.isBefore(now.plus(window))
