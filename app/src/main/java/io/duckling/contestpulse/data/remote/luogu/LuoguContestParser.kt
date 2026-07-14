package io.duckling.contestpulse.data.remote.luogu

import io.duckling.contestpulse.data.remote.RemoteParsingException
import io.duckling.contestpulse.domain.logic.stableContestId
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.model.ContestStatus
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale
import javax.inject.Inject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class LuoguContestParser @Inject constructor() {
    fun parse(
        html: String,
        fetchedAt: Instant,
    ): List<Contest> {
        val document = Jsoup.parse(html, LUOGU_BASE_URL)
        val contestList = document.selectFirst(CONTEST_LIST_SELECTOR)
            ?: throw RemoteParsingException("Luogu contest list was not found")
        val rows = contestList.children().filter { element -> element.tagName() == "li" }
        val parsedRows = rows.mapNotNull { row -> row.toContestOrNull(fetchedAt) }
        if (parsedRows.size != rows.size) {
            throw RemoteParsingException("Some Luogu contest rows could not be parsed")
        }
        return parsedRows
            .filter { contest -> contest.endTime?.isAfter(fetchedAt) == true }
            .distinctBy(Contest::id)
            .sortedBy(Contest::startTime)
    }
}

private fun Element.toContestOrNull(fetchedAt: Instant): Contest? {
    val contestLink = selectFirst("h3 > a[href^=/contest/]") ?: return null
    val path = contestLink.attr("href").trim()
    val sourceContestId = path.removePrefix("/contest/")
        .takeIf { id -> id.matches(CONTEST_ID_PATTERN) }
        ?: return null
    val title = contestLink.text().trim().takeIf(String::isNotEmpty) ?: return null
    val timeRange = selectFirst("p > small")?.text()?.trim() ?: return null
    val rangeMatch = TIME_RANGE_PATTERN.matchEntire(timeRange) ?: return null
    val startTime = parseChinaTime(rangeMatch.groupValues[1]) ?: return null
    val endTime = parseChinaTime(rangeMatch.groupValues[2]) ?: return null
    if (!endTime.isAfter(startTime)) return null
    val duration = try {
        Duration.between(startTime, endTime)
    } catch (_: DateTimeException) {
        return null
    } catch (_: ArithmeticException) {
        return null
    }
    val contestUrl = "$LUOGU_BASE_URL$path"

    return Contest(
        id = stableContestId(ContestSource.LUOGU, sourceContestId),
        source = ContestSource.LUOGU,
        sourceContestId = sourceContestId,
        title = title,
        startTime = startTime,
        endTime = endTime,
        duration = duration,
        registrationUrl = contestUrl,
        contestUrl = contestUrl,
        status = when {
            !startTime.isAfter(fetchedAt) && endTime.isAfter(fetchedAt) -> ContestStatus.RUNNING
            startTime.isAfter(fetchedAt) -> ContestStatus.UPCOMING
            else -> ContestStatus.FINISHED
        },
        category = luoguCategory(title),
        difficultyLabel = DIVISION_PATTERN.find(title)?.value,
        ratedRange = null,
        isRated = null,
        isFavorite = false,
        reminderOffsets = emptySet(),
        lastUpdatedAt = fetchedAt,
    )
}

private fun parseChinaTime(value: String): Instant? = try {
    LocalDateTime.parse(value.trim(), CONTEST_TIME_FORMATTER)
        .atZone(CHINA_ZONE_ID)
        .toInstant()
} catch (_: DateTimeParseException) {
    null
} catch (_: DateTimeException) {
    null
}

private fun luoguCategory(title: String): String? = when {
    title.contains("入门赛") -> "入门赛"
    title.contains("基础赛") -> "基础赛"
    title.contains("月赛") -> "月赛"
    title.contains("重现赛") -> "重现赛"
    else -> null
}

private const val LUOGU_BASE_URL = "https://www.luogu.com.cn"
private const val CONTEST_LIST_SELECTOR = "#app > ul"
private val CHINA_ZONE_ID: ZoneId = ZoneId.of("Asia/Shanghai")
private val CONTEST_ID_PATTERN = Regex("[0-9]+")
private val TIME_RANGE_PATTERN = Regex("(.+?)\\s*~\\s*(.+)")
private val DIVISION_PATTERN = Regex("Div\\.[1-4]", RegexOption.IGNORE_CASE)
private val CONTEST_TIME_FORMATTER = DateTimeFormatter.ofPattern(
    "uuuu-MM-dd HH:mm:ss",
    Locale.SIMPLIFIED_CHINESE,
).withResolverStyle(ResolverStyle.STRICT)
