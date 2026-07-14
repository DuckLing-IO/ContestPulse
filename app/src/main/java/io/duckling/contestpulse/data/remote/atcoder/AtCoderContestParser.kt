package io.duckling.contestpulse.data.remote.atcoder

import io.duckling.contestpulse.data.remote.RemoteParsingException
import io.duckling.contestpulse.domain.logic.stableContestId
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.model.ContestStatus
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.inject.Inject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class AtCoderContestParser @Inject constructor() {
    fun parse(
        html: String,
        fetchedAt: Instant,
    ): List<Contest> {
        val document = Jsoup.parse(html, ATCODER_BASE_URL)
        val sections = document.select(SECTION_SELECTOR)
        if (sections.isEmpty()) {
            throw RemoteParsingException("AtCoder contest sections were not found")
        }
        val rows = sections.select("tbody tr")
        val contests = rows
            .mapNotNull { row -> row.toContestOrNull(fetchedAt) }
            .distinctBy(Contest::id)
            .sortedBy(Contest::startTime)
        if (rows.isNotEmpty() && contests.isEmpty()) {
            throw RemoteParsingException("AtCoder contest rows could not be parsed")
        }
        return contests
    }
}

private fun Element.toContestOrNull(fetchedAt: Instant): Contest? {
    val cells = select("td")
    if (cells.size < EXPECTED_COLUMN_COUNT) return null

    val timeText = cells[START_TIME_COLUMN].selectFirst("time")
        ?.text()
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?: return null
    val contestLink = cells[NAME_COLUMN].selectFirst("a[href^=/contests/]") ?: return null
    val path = contestLink.attr("href").trim()
    val sourceContestId = path.removePrefix("/contests/")
        .takeIf { id -> id.matches(CONTEST_ID_PATTERN) }
        ?: return null
    val title = contestLink.text().trim().takeIf(String::isNotEmpty) ?: return null
    val duration = parseDuration(cells[DURATION_COLUMN].text()) ?: return null
    val startTime = parseStartTime(timeText) ?: return null
    val endTime = try {
        startTime.plus(duration)
    } catch (_: DateTimeException) {
        return null
    } catch (_: ArithmeticException) {
        return null
    }
    if (!endTime.isAfter(fetchedAt)) return null

    val ratedText = cells[RATED_RANGE_COLUMN].text().trim()
    val ratedRange = ratedText.takeUnless { value -> value.isBlank() || value == "-" }
    val category = cells[NAME_COLUMN]
        .selectFirst("span[title]")
        ?.attr("title")
        ?.trim()
        ?.takeIf(String::isNotEmpty)

    return Contest(
        id = stableContestId(ContestSource.ATCODER, sourceContestId),
        source = ContestSource.ATCODER,
        sourceContestId = sourceContestId,
        title = title,
        startTime = startTime,
        endTime = endTime,
        duration = duration,
        registrationUrl = null,
        contestUrl = "$ATCODER_BASE_URL$path",
        status = if (startTime.isAfter(fetchedAt)) {
            ContestStatus.UPCOMING
        } else {
            ContestStatus.RUNNING
        },
        category = category,
        difficultyLabel = contestSeries(title),
        ratedRange = ratedRange,
        isRated = ratedRange != null,
        isFavorite = false,
        reminderOffsets = emptySet(),
        lastUpdatedAt = fetchedAt,
    )
}

private fun parseStartTime(value: String): Instant? = try {
    OffsetDateTime.parse(value, START_TIME_FORMATTER).toInstant()
} catch (_: DateTimeParseException) {
    null
}

private fun parseDuration(value: String): Duration? {
    val match = DURATION_PATTERN.matchEntire(value.trim()) ?: return null
    val hours = match.groupValues[1].toLongOrNull() ?: return null
    val minutes = match.groupValues[2].toLongOrNull()?.takeIf { it in 0..59 } ?: return null
    return try {
        Duration.ofHours(hours).plusMinutes(minutes).takeIf { !it.isZero }
    } catch (_: ArithmeticException) {
        null
    }
}

private fun contestSeries(title: String): String? = when {
    title.contains("AtCoder Beginner Contest", ignoreCase = true) -> "ABC"
    title.contains("AtCoder Regular Contest", ignoreCase = true) -> "ARC"
    title.contains("AtCoder Grand Contest", ignoreCase = true) -> "AGC"
    title.contains("AtCoder Heuristic Contest", ignoreCase = true) -> "AHC"
    else -> null
}

private const val ATCODER_BASE_URL = "https://atcoder.jp"
private const val SECTION_SELECTOR = "#contest-table-upcoming, #contest-table-daily"
private const val EXPECTED_COLUMN_COUNT = 4
private const val START_TIME_COLUMN = 0
private const val NAME_COLUMN = 1
private const val DURATION_COLUMN = 2
private const val RATED_RANGE_COLUMN = 3
private val CONTEST_ID_PATTERN = Regex("[A-Za-z0-9_-]+")
private val DURATION_PATTERN = Regex("(\\d+):(\\d{2})")
private val START_TIME_FORMATTER = DateTimeFormatter.ofPattern(
    "yyyy-MM-dd HH:mm:ssZ",
    Locale.US,
)
