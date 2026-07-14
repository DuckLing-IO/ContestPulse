package io.duckling.contestpulse.data.remote.nowcoder

import io.duckling.contestpulse.data.remote.RemoteParsingException
import io.duckling.contestpulse.domain.logic.stableContestId
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.model.ContestStatus
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

class NowcoderContestParser @Inject constructor(
    private val json: Json,
) {
    fun parse(
        html: String,
        fetchedAt: Instant,
    ): List<Contest> {
        val document = Jsoup.parse(html, NOWCODER_BASE_URL)
        val currentSection = document.selectFirst(CURRENT_CONTEST_SECTION_SELECTOR)
            ?: throw RemoteParsingException("Nowcoder current contest section was not found")
        val rows = currentSection.select(CONTEST_ROW_SELECTOR)
        val parsedRows = rows.mapNotNull { row -> row.toContestOrNull(json, fetchedAt) }
        if (parsedRows.size != rows.size) {
            throw RemoteParsingException("Some Nowcoder contest rows could not be parsed")
        }
        return parsedRows
            .filter { contest -> contest.endTime?.isAfter(fetchedAt) == true }
            .distinctBy(Contest::id)
            .sortedBy(Contest::startTime)
    }
}

private fun Element.toContestOrNull(
    json: Json,
    fetchedAt: Instant,
): Contest? {
    val payload = try {
        json.decodeFromString<NowcoderContestPayload>(
            attr("data-json").decodeNestedHtmlEntities(),
        )
    } catch (_: SerializationException) {
        return null
    } catch (_: IllegalArgumentException) {
        return null
    }
    val sourceContestId = payload.contestId?.toString() ?: return null
    if (sourceContestId != attr("data-id").trim()) return null
    val contestLink = selectFirst("h4 > a[href^=/acm/contest/]") ?: return null
    val path = contestLink.attr("href").trim()
    val pathId = path.removePrefix("/acm/contest/")
        .takeIf { id -> id.matches(CONTEST_ID_PATTERN) }
        ?: return null
    if (pathId != sourceContestId) return null
    val title = contestLink.text().trim().takeIf(String::isNotEmpty) ?: return null
    val payloadTitle = payload.contestName?.trim()?.takeIf(String::isNotEmpty) ?: return null
    if (payloadTitle.normalizedWhitespace() != title.normalizedWhitespace()) return null
    val startTime = payload.contestStartTime?.toInstantOrNull() ?: return null
    val endTime = payload.contestEndTime?.toInstantOrNull() ?: return null
    if (!endTime.isAfter(startTime)) return null
    val duration = try {
        Duration.between(startTime, endTime)
    } catch (_: DateTimeException) {
        return null
    } catch (_: ArithmeticException) {
        return null
    }
    if (payload.contestDuration == null || payload.contestDuration <= 0L) return null
    val durationMillis = try {
        duration.toMillis()
    } catch (_: ArithmeticException) {
        return null
    }
    if (durationMillis != payload.contestDuration) return null
    val contestUrl = "$NOWCODER_BASE_URL$path"
    val isRated = selectFirst(".tag-rating") != null
    val ratingUpperLimit = payload.settingInfo
        ?.takeIf { settings -> settings.needRatingUpperLimit == true }
        ?.ratingUpperLimit

    return Contest(
        id = stableContestId(ContestSource.NOWCODER, sourceContestId),
        source = ContestSource.NOWCODER,
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
        category = nowcoderCategory(payload.topCategoryId),
        difficultyLabel = null,
        ratedRange = ratingUpperLimit?.let { upperLimit -> "≤ $upperLimit" },
        isRated = isRated,
        isFavorite = false,
        reminderOffsets = emptySet(),
        lastUpdatedAt = fetchedAt,
    )
}

private fun Long.toInstantOrNull(): Instant? = try {
    Instant.ofEpochMilli(this)
} catch (_: DateTimeException) {
    null
}

private fun String.normalizedWhitespace(): String = replace(WHITESPACE_PATTERN, " ").trim()

private fun String.decodeNestedHtmlEntities(): String {
    var decoded = this
    repeat(MAX_ENTITY_DECODE_PASSES) {
        val next = Parser.unescapeEntities(decoded, true)
        if (next == decoded) return decoded
        decoded = next
    }
    return decoded
}

private fun nowcoderCategory(topCategoryId: Int?): String? = when (topCategoryId) {
    13 -> "牛客系列赛"
    14 -> "高校校赛"
    else -> null
}

@Serializable
private data class NowcoderContestPayload(
    val contestId: Long? = null,
    val contestName: String? = null,
    val contestStartTime: Long? = null,
    val contestEndTime: Long? = null,
    val contestDuration: Long? = null,
    val topCategoryId: Int? = null,
    val settingInfo: NowcoderContestSettings? = null,
)

@Serializable
private data class NowcoderContestSettings(
    val needRatingUpperLimit: Boolean? = null,
    val ratingUpperLimit: Int? = null,
)

private const val NOWCODER_BASE_URL = "https://ac.nowcoder.com"
private const val CURRENT_CONTEST_SECTION_SELECTOR = ".platform-mod.js-current"
private const val CONTEST_ROW_SELECTOR = ".platform-item.js-item[data-id][data-json]"
private const val MAX_ENTITY_DECODE_PASSES = 2
private val CONTEST_ID_PATTERN = Regex("[0-9]+")
private val WHITESPACE_PATTERN = Regex("\\s+")
