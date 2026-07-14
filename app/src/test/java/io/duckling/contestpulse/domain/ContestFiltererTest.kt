package io.duckling.contestpulse.domain

import io.duckling.contestpulse.domain.logic.filterContests
import io.duckling.contestpulse.domain.model.ContestFilter
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.model.ContestDateRange
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ContestFiltererTest {
    private val contests = listOf(
        testContest(
            sourceContestId = "cf-1",
            title = "Demo Round",
            source = ContestSource.CODEFORCES,
            isRated = true,
            isFavorite = true,
        ),
        testContest(
            sourceContestId = "ac-1",
            title = "Beginner Practice",
            source = ContestSource.ATCODER,
            isRated = false,
            isFavorite = false,
        ),
    )

    @Test
    fun appliesSourceFavoriteAndRatedTogether() {
        val result = filterContests(
            contests = contests,
            filter = ContestFilter(
                sources = setOf(ContestSource.CODEFORCES),
                favoriteOnly = true,
                ratedOnly = true,
            ),
        )

        assertEquals(listOf("cf-1"), result.map { it.sourceContestId })
    }

    @Test
    fun emptySourceSelection_returnsNoResults() {
        val result = filterContests(
            contests = contests,
            filter = ContestFilter(sources = emptySet()),
        )

        assertEquals(emptyList<Any>(), result)
    }

    @Test
    fun dateRange_excludesPastAndLaterContests() {
        val now = Instant.parse("2026-07-14T10:00:00Z")
        val result = filterContests(
            contests = listOf(
                testContest(sourceContestId = "past", startTime = now.minusSeconds(60)),
                testContest(sourceContestId = "week", startTime = now.plusSeconds(86_400)),
                testContest(sourceContestId = "later", startTime = now.plusSeconds(864_000)),
            ),
            filter = ContestFilter(dateRange = ContestDateRange.NEXT_7_DAYS),
            now = now,
        )

        assertEquals(listOf("week"), result.map { it.sourceContestId })
    }
}
