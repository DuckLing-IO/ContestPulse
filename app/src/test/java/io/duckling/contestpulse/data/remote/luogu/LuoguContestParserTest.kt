package io.duckling.contestpulse.data.remote.luogu

import io.duckling.contestpulse.data.remote.RemoteParsingException
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.model.ContestStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LuoguContestParserTest {
    private val parser = LuoguContestParser()
    private val fetchedAt = Instant.parse("2026-07-14T10:00:00Z")
    private val fixture = requireNotNull(
        javaClass.classLoader?.getResource("fixtures/luogu_contests.html"),
    ).readText()

    @Test
    fun officialListRow_mapsChinaTimeDurationAndMetadata() {
        val contest = parser.parse(fixture, fetchedAt)
            .first { item -> item.sourceContestId == "325521" }

        assertEquals("luogu:325521", contest.id)
        assertEquals(ContestSource.LUOGU, contest.source)
        assertEquals(ContestStatus.UPCOMING, contest.status)
        assertEquals(Instant.parse("2026-08-01T06:00:00Z"), contest.startTime)
        assertEquals(210L, contest.duration?.toMinutes())
        assertEquals("基础赛", contest.category)
        assertEquals("Div.3", contest.difficultyLabel)
        assertEquals("https://www.luogu.com.cn/contest/325521", contest.contestUrl)
    }

    @Test
    fun finishedRows_areExcluded() {
        val contests = parser.parse(fixture, fetchedAt)

        assertEquals(listOf("332970", "325521"), contests.map { it.sourceContestId })
    }

    @Test
    fun missingContestList_isReportedAsParsingFailure() {
        assertThrows(RemoteParsingException::class.java) {
            parser.parse("<html><body></body></html>", fetchedAt)
        }
    }

    @Test
    fun malformedKnownRows_areReportedAsParsingFailure() {
        val malformed = """
            <div id="app"><ul><li>
                <h3><a href="/contest/325521">Contest</a></h3>
                <p><small>not-a-time</small></p>
            </li></ul></div>
        """.trimIndent()

        assertThrows(RemoteParsingException::class.java) {
            parser.parse(malformed, fetchedAt)
        }
    }

    @Test
    fun emptyRecognizedList_isValid() {
        assertTrue(parser.parse("<div id=\"app\"><ul></ul></div>", fetchedAt).isEmpty())
    }

    @Test
    fun invalidContestUrl_failsClosed() {
        val invalid = """
            <div id="app"><ul><li>
                <h3><a href="javascript:alert(1)">Contest</a></h3>
                <p><small>2026-08-01 14:00:00 ~ 2026-08-01 17:30:00</small></p>
            </li></ul></div>
        """.trimIndent()

        assertThrows(RemoteParsingException::class.java) {
            parser.parse(invalid, fetchedAt)
        }
    }
}
