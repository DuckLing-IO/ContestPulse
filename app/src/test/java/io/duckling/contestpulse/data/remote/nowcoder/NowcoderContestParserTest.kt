package io.duckling.contestpulse.data.remote.nowcoder

import io.duckling.contestpulse.data.remote.RemoteParsingException
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.model.ContestStatus
import java.time.Instant
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NowcoderContestParserTest {
    private val parser = NowcoderContestParser(
        Json { ignoreUnknownKeys = true },
    )
    private val fetchedAt = Instant.parse("2026-07-14T10:00:00Z")
    private val fixture = requireNotNull(
        javaClass.classLoader?.getResource("fixtures/nowcoder_contests.html"),
    ).readText()

    @Test
    fun publicContestRow_mapsEpochTimesRatedRangeAndCategory() {
        val contest = parser.parse(fixture, fetchedAt).first()

        assertEquals("nowcoder:133876", contest.id)
        assertEquals(ContestSource.NOWCODER, contest.source)
        assertEquals(ContestStatus.UPCOMING, contest.status)
        assertEquals(Instant.parse("2026-07-17T04:00:00Z"), contest.startTime)
        assertEquals(300L, contest.duration?.toMinutes())
        assertEquals("牛客系列赛", contest.category)
        assertEquals("≤ 2399", contest.ratedRange)
        assertTrue(contest.isRated == true)
        assertEquals("https://ac.nowcoder.com/acm/contest/133876", contest.contestUrl)
    }

    @Test
    fun schoolContestAndFinishedRows_areHandled() {
        val contests = parser.parse(fixture, fetchedAt)

        assertEquals(listOf("133876", "137134"), contests.map { it.sourceContestId })
        assertEquals("高校校赛", contests.last().category)
        assertFalse(contests.last().isRated == true)
    }

    @Test
    fun missingCurrentSection_isReportedAsParsingFailure() {
        assertThrows(RemoteParsingException::class.java) {
            parser.parse("<html><body></body></html>", fetchedAt)
        }
    }

    @Test
    fun malformedKnownRows_areReportedAsParsingFailure() {
        val malformed = """
            <div class="platform-mod js-current">
                <div class="platform-item js-item" data-id="1" data-json="not-json">
                    <h4><a href="/acm/contest/1">Contest</a></h4>
                </div>
            </div>
        """.trimIndent()

        assertThrows(RemoteParsingException::class.java) {
            parser.parse(malformed, fetchedAt)
        }
    }

    @Test
    fun emptyRecognizedSection_isValid() {
        val html = "<div class=\"platform-mod js-current\"></div>"
        assertTrue(parser.parse(html, fetchedAt).isEmpty())
    }

    @Test
    fun mismatchedContestId_failsClosed() {
        val invalid = fixture
            .substringBefore("<div\n        data-id=\"137134\"")
            .replace("data-id=\"133876\"", "data-id=\"999999\"") + "</div></body></html>"

        assertThrows(RemoteParsingException::class.java) {
            parser.parse(invalid, fetchedAt)
        }
    }

    @Test
    fun mismatchedDuration_failsClosed() {
        val invalid = fixture.replace(
            "&amp;quot;contestDuration&amp;quot;:18000000",
            "&amp;quot;contestDuration&amp;quot;:60000",
        )

        assertThrows(RemoteParsingException::class.java) {
            parser.parse(invalid, fetchedAt)
        }
    }
}
