package io.duckling.contestpulse.data.remote.atcoder

import io.duckling.contestpulse.data.remote.RemoteParsingException
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.model.ContestStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AtCoderContestParserTest {
    private val parser = AtCoderContestParser()
    private val fetchedAt = Instant.parse("2026-07-14T10:00:00Z")
    private val fixture = requireNotNull(
        javaClass.classLoader?.getResource("fixtures/atcoder_contests.html"),
    ).readText()

    @Test
    fun officialTableRow_mapsTimeDurationAndRatedRange() {
        val contest = parser.parse(fixture, fetchedAt).first()

        assertEquals("atcoder:abc467", contest.id)
        assertEquals(ContestSource.ATCODER, contest.source)
        assertEquals(ContestStatus.UPCOMING, contest.status)
        assertEquals(Instant.parse("2026-07-18T12:00:00Z"), contest.startTime)
        assertEquals(100L, contest.duration?.toMinutes())
        assertEquals("- 1999", contest.ratedRange)
        assertEquals("ABC", contest.difficultyLabel)
        assertEquals("Algorithm", contest.category)
        assertTrue(contest.isRated == true)
    }

    @Test
    fun longHeuristicDuration_isSupported() {
        val html = fixture
            .replace("abc467", "ahc069")
            .replace("AtCoder Beginner Contest 467", "AtCoder Heuristic Contest 069")
            .replace("01:40", "240:00")
            .replace("- 1999", "All")
            .replace("Algorithm", "Heuristic")

        val contest = parser.parse(html, fetchedAt).first()

        assertEquals(240L, contest.duration?.toHours())
        assertEquals("AHC", contest.difficultyLabel)
    }

    @Test
    fun missingKnownSections_isReportedAsParsingFailure() {
        assertThrows(RemoteParsingException::class.java) {
            parser.parse("<html><body></body></html>", fetchedAt)
        }
    }

    @Test
    fun malformedKnownRows_areReportedAsParsingFailure() {
        val malformed = fixture.replace(
            "2026-07-18 21:00:00+0900",
            "not-a-time",
        )
        assertThrows(RemoteParsingException::class.java) {
            parser.parse(malformed, fetchedAt)
        }
    }

    @Test
    fun emptyRecognizedTables_areValid() {
        val empty = fixture.replace(
            Regex("<tr>.*?</tr>", setOf(RegexOption.DOT_MATCHES_ALL)),
            "",
        )
        assertTrue(parser.parse(empty, fetchedAt).isEmpty())
    }

    @Test
    fun invalidContestUrlAndMissingColumn_failClosed() {
        val invalidUrl = fixture.replace("/contests/abc467", "javascript:alert(1)")
        assertThrows(RemoteParsingException::class.java) {
            parser.parse(invalidUrl, fetchedAt)
        }

        val missingDuration = fixture.replace("<td>01:40</td>", "")
        assertThrows(RemoteParsingException::class.java) {
            parser.parse(missingDuration, fetchedAt)
        }
    }
}
