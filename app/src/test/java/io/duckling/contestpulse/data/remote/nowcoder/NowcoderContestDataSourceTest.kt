package io.duckling.contestpulse.data.remote.nowcoder

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class NowcoderContestDataSourceTest {
    private val fetchedAt = Instant.parse("2026-07-14T10:00:00Z")
    private val fixture = requireNotNull(
        javaClass.classLoader?.getResource("fixtures/nowcoder_contests.html"),
    ).readText()

    @Test
    fun fetchesSeriesAndSchoolCategories_thenDeduplicates() = runBlocking {
        val api = RecordingNowcoderApi(fixture)
        val dataSource = NowcoderContestDataSource(
            api = api,
            parser = NowcoderContestParser(Json { ignoreUnknownKeys = true }),
            clock = Clock.fixed(fetchedAt, ZoneOffset.UTC),
        )

        val contests = dataSource.fetchUpcomingContests()

        assertEquals(setOf(13, 14), api.requestedCategories.toSet())
        assertEquals(listOf("133876", "137134"), contests.map { it.sourceContestId })
    }
}

private class RecordingNowcoderApi(
    private val html: String,
) : NowcoderContestPageApi {
    val requestedCategories = mutableListOf<Int>()

    override suspend fun getContestsPage(topCategoryFilter: Int): ResponseBody {
        synchronized(requestedCategories) {
            requestedCategories += topCategoryFilter
        }
        return html.toResponseBody("text/html; charset=UTF-8".toMediaType())
    }
}
