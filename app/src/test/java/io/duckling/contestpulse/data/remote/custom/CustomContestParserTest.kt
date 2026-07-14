package io.duckling.contestpulse.data.remote.custom

import io.duckling.contestpulse.domain.customsource.CustomContestSource
import io.duckling.contestpulse.domain.customsource.CustomHtmlSelectors
import io.duckling.contestpulse.domain.customsource.CustomSourceFormat
import java.time.Instant
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomContestParserTest {
    private val parser = CustomContestParser(Json { ignoreUnknownKeys = true })
    private val fetchedAt = Instant.parse("2026-07-14T00:00:00Z")

    @Test
    fun genericJson_extractsUpcomingContest() {
        val result = parser.parse(
            source = source(format = CustomSourceFormat.JSON),
            fetchedContent = content(
                """
                {"contests":[{
                  "id":"round-1",
                  "title":"Example Round 1",
                  "start_time":"2026-07-20T12:00:00Z",
                  "end_time":"2026-07-20T14:00:00Z",
                  "url":"/contest/round-1"
                }]}
                """.trimIndent(),
                contentType = "application/json",
            ),
            fetchedAt = fetchedAt,
        )

        assertEquals(CustomSourceFormat.JSON, result.detectedFormat)
        assertEquals("Example Round 1", result.contests.single().title)
        assertEquals("https://example.com/contest/round-1", result.contests.single().contestUrl)
        assertEquals(120L, result.contests.single().duration?.toMinutes())
    }

    @Test
    fun nowcoderStyleDoubleEncodedDataJson_isAutoDetected() {
        val html = """
            <div class="platform-item" data-id="133876"
              data-json="{&amp;quot;contestDuration&amp;quot;:7200000,&amp;quot;contestEndTime&amp;quot;:1784466000000,&amp;quot;contestStartTime&amp;quot;:1784458800000,&amp;quot;contestName&amp;quot;:&amp;quot;牛客周赛&amp;quot;,&amp;quot;contestId&amp;quot;:133876}">
              <h4><a href="/acm/contest/133876">牛客周赛</a></h4>
            </div>
        """.trimIndent()

        val result = parser.parse(source(), content(html), fetchedAt)

        assertEquals(CustomSourceFormat.HTML, result.detectedFormat)
        assertEquals("牛客周赛", result.contests.single().title)
        assertEquals("https://example.com/acm/contest/133876", result.contests.single().contestUrl)
    }

    @Test
    fun mappedHtml_usesCssSelectorsPatternAndTimezone() {
        val configured = source(
            format = CustomSourceFormat.HTML,
            selectors = CustomHtmlSelectors(
                item = ".contest",
                title = ".title",
                start = ".start",
                end = ".end",
                link = "a",
                dateTimePattern = "yyyy/MM/dd HH:mm",
            ),
        )
        val html = """
            <article class="contest" data-id="abc">
              <a href="/c/abc"><span class="title">校内赛</span></a>
              <time class="start">2026/07/21 19:00</time>
              <time class="end">2026/07/21 21:00</time>
            </article>
        """.trimIndent()

        val contest = parser.parse(configured, content(html), fetchedAt).contests.single()

        assertEquals(Instant.parse("2026-07-21T11:00:00Z"), contest.startTime)
        assertEquals("校内赛", contest.title)
    }

    @Test
    fun iCalendar_extractsEventAndTimezone() {
        val calendar = """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            UID:weekly-1@example.com
            SUMMARY:Weekly Contest
            DTSTART;TZID=Asia/Shanghai:20260725T190000
            DTEND;TZID=Asia/Shanghai:20260725T210000
            URL:https://example.com/contest/weekly-1
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = parser.parse(
            source(format = CustomSourceFormat.ICALENDAR),
            content(calendar, "text/calendar"),
            fetchedAt,
        )

        assertEquals(CustomSourceFormat.ICALENDAR, result.detectedFormat)
        assertEquals(Instant.parse("2026-07-25T11:00:00Z"), result.contests.single().startTime)
        assertTrue(result.warnings.isEmpty())
    }

    private fun source(
        format: CustomSourceFormat = CustomSourceFormat.AUTO,
        selectors: CustomHtmlSelectors = CustomHtmlSelectors(),
    ) = CustomContestSource(
        id = "source-1234",
        name = "示例平台",
        url = "https://example.com/contests",
        format = format,
        selectors = selectors,
    )

    private fun content(
        body: String,
        contentType: String = "text/html; charset=UTF-8",
    ) = FetchedCustomSourceContent(
        body = body,
        finalUrl = "https://example.com/contests",
        contentType = contentType,
    )
}
