package io.duckling.contestpulse.data.remote.codeforces

import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.model.ContestStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class CodeforcesContestMapperTest {
    private val fetchedAt = Instant.parse("2026-07-14T10:00:00Z")

    @Test
    fun beforeContest_mapsOfficialFields() {
        val contest = CodeforcesContestDto(
            id = 2163,
            name = "Codeforces Round 2163",
            type = "CF",
            phase = "BEFORE",
            durationSeconds = 7_200,
            startTimeSeconds = 1_784_112_400,
        ).toDomainOrNull(fetchedAt)

        requireNotNull(contest)
        assertEquals("codeforces:2163", contest.id)
        assertEquals(ContestSource.CODEFORCES, contest.source)
        assertEquals(ContestStatus.UPCOMING, contest.status)
        assertEquals("https://codeforces.com/contest/2163", contest.contestUrl)
        assertEquals(120L, contest.duration?.toMinutes())
        assertEquals(fetchedAt, contest.lastUpdatedAt)
    }

    @Test
    fun codingContest_mapsAsRunning() {
        val contest = validDto(phase = "CODING").toDomainOrNull(fetchedAt)

        assertEquals(ContestStatus.RUNNING, contest?.status)
    }

    @Test
    fun finishedAndMalformedRecords_areDiscarded() {
        assertNull(validDto(phase = "FINISHED").toDomainOrNull(fetchedAt))
        assertNull(validDto().copy(name = " ").toDomainOrNull(fetchedAt))
        assertNull(validDto().copy(startTimeSeconds = null).toDomainOrNull(fetchedAt))
        assertTrue(validDto().toDomainOrNull(fetchedAt) != null)
    }

    @Test
    fun officialJsonFixture_ignoresUnneededFieldsAndMapsContest() {
        val fixture = requireNotNull(
            javaClass.classLoader?.getResource("fixtures/codeforces_contests.json"),
        ).readText()
        val response = Json { ignoreUnknownKeys = true }
            .decodeFromString<CodeforcesContestResponse>(fixture)

        assertEquals("OK", response.status)
        assertEquals("codeforces:2163", response.result?.single()?.toDomainOrNull(fetchedAt)?.id)
    }

    private fun validDto(phase: String = "BEFORE") = CodeforcesContestDto(
        id = 1,
        name = "Contest",
        type = "CF",
        phase = phase,
        durationSeconds = 7_200,
        startTimeSeconds = 1_784_112_400,
    )
}
