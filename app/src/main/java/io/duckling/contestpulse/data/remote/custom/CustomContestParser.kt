package io.duckling.contestpulse.data.remote.custom

import io.duckling.contestpulse.data.remote.RemoteParsingException
import io.duckling.contestpulse.domain.customsource.CustomContestSource
import io.duckling.contestpulse.domain.customsource.CustomSourceFormat
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.model.ContestStatus
import java.net.URI
import java.security.MessageDigest
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.jsoup.select.Selector

data class CustomSourceParseResult(
    val contests: List<Contest>,
    val detectedFormat: CustomSourceFormat,
    val warnings: List<String>,
)

class CustomContestParser @Inject constructor(
    private val json: Json,
) {
    fun parse(
        source: CustomContestSource,
        fetchedContent: FetchedCustomSourceContent,
        fetchedAt: Instant,
    ): CustomSourceParseResult {
        source.requireValid()
        val zoneId = ZoneId.of(source.timezoneId)
        val parsed = when {
            source.selectors.hasAnyValue -> parseHtml(source, fetchedContent, fetchedAt, zoneId)
            source.format == CustomSourceFormat.JSON -> parseJson(source, fetchedContent, fetchedAt, zoneId)
            source.format == CustomSourceFormat.ICALENDAR -> {
                parseICalendar(source, fetchedContent, fetchedAt, zoneId)
            }
            source.format == CustomSourceFormat.HTML -> parseHtml(source, fetchedContent, fetchedAt, zoneId)
            else -> parseAutomatically(source, fetchedContent, fetchedAt, zoneId)
        }
        if (parsed.contests.isEmpty()) {
            throw RemoteParsingException("No upcoming contests could be extracted from the custom source")
        }
        return parsed
    }

    private fun parseAutomatically(
        source: CustomContestSource,
        content: FetchedCustomSourceContent,
        fetchedAt: Instant,
        zoneId: ZoneId,
    ): CustomSourceParseResult {
        val body = content.body.trimStart()
        val contentType = content.contentType.orEmpty().lowercase(Locale.ROOT)
        if (body.startsWith("BEGIN:VCALENDAR") || "text/calendar" in contentType) {
            return parseICalendar(source, content, fetchedAt, zoneId)
        }
        if (body.startsWith("{") || body.startsWith("[") || "json" in contentType) {
            runCatching { parseJson(source, content, fetchedAt, zoneId) }
                .getOrNull()
                ?.takeIf { result -> result.contests.isNotEmpty() }
                ?.let { return it }
        }
        return parseHtml(source, content, fetchedAt, zoneId)
    }

    private fun parseJson(
        source: CustomContestSource,
        content: FetchedCustomSourceContent,
        fetchedAt: Instant,
        zoneId: ZoneId,
    ): CustomSourceParseResult {
        val root = try {
            json.parseToJsonElement(content.body)
        } catch (exception: IllegalArgumentException) {
            throw RemoteParsingException("Custom source did not return valid JSON")
        }
        val candidates = root.collectContestObjects()
        val parsedCandidates = candidates.map { candidate ->
            candidate.toContest(
                source = source,
                baseUrl = content.finalUrl,
                fetchedAt = fetchedAt,
                zoneId = zoneId,
                dateTimePattern = source.selectors.dateTimePattern,
            )
        }
        if (parsedCandidates.any { contest -> contest == null }) {
            throw RemoteParsingException("Some contest objects in the custom JSON are invalid")
        }
        return buildResult(
            candidates = parsedCandidates.filterNotNull(),
            format = CustomSourceFormat.JSON,
            fetchedAt = fetchedAt,
        )
    }

    private fun parseHtml(
        source: CustomContestSource,
        content: FetchedCustomSourceContent,
        fetchedAt: Instant,
        zoneId: ZoneId,
    ): CustomSourceParseResult {
        val document = Jsoup.parse(content.body, content.finalUrl)
        val contests = try {
            when {
                source.selectors.hasAnyValue -> {
                    val rows = document.select(source.selectors.item)
                    val mapped = rows.map { row ->
                        row.toMappedContest(source, fetchedAt, zoneId, content.finalUrl)
                    }
                    if (mapped.any { contest -> contest == null }) {
                        throw RemoteParsingException("Some rows selected by the custom mapping are invalid")
                    }
                    mapped.filterNotNull()
                }
                else -> {
                    val jsonLdCandidates = document.select("script[type=application/ld+json]")
                        .flatMap { script ->
                            runCatching { json.parseToJsonElement(script.data()) }
                                .getOrNull()
                                ?.collectContestObjects()
                                .orEmpty()
                        }
                    val jsonLd = jsonLdCandidates.map { candidate ->
                            candidate.toContest(
                                source = source,
                                baseUrl = content.finalUrl,
                                fetchedAt = fetchedAt,
                                zoneId = zoneId,
                                dateTimePattern = "",
                            )
                        }
                    if (jsonLd.any { contest -> contest == null }) {
                        throw RemoteParsingException("Some JSON-LD events in the custom page are invalid")
                    }
                    if (jsonLd.isNotEmpty()) {
                        jsonLd.filterNotNull()
                    } else {
                        val embedded = document.select("[data-json]")
                            .mapNotNull { row ->
                                row.toEmbeddedJsonContest(
                                    source = source,
                                    fetchedAt = fetchedAt,
                                    zoneId = zoneId,
                                    baseUrl = content.finalUrl,
                                )
                            }
                        if (embedded.isNotEmpty()) embedded else document.semanticTimeContests(
                            source = source,
                            fetchedAt = fetchedAt,
                            zoneId = zoneId,
                            baseUrl = content.finalUrl,
                        )
                    }
                }
            }
        } catch (exception: Selector.SelectorParseException) {
            throw RemoteParsingException("A custom CSS selector is invalid")
        }
        return buildResult(contests, CustomSourceFormat.HTML, fetchedAt)
    }

    private fun parseICalendar(
        source: CustomContestSource,
        content: FetchedCustomSourceContent,
        fetchedAt: Instant,
        zoneId: ZoneId,
    ): CustomSourceParseResult {
        val unfolded = content.body
            .replace("\r\n", "\n")
            .replace(Regex("\n[ \t]"), "")
        val events = ICALENDAR_EVENT_PATTERN.findAll(unfolded).mapNotNull { match ->
            val properties = match.groupValues[1].lineSequence()
                .mapNotNull(::parseCalendarProperty)
                .groupBy({ property -> property.name }, { property -> property })
            val title = properties["SUMMARY"]?.firstOrNull()?.value
                ?.unescapeICalendarText()
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: return@mapNotNull null
            val startProperty = properties["DTSTART"]?.firstOrNull() ?: return@mapNotNull null
            val start = startProperty.toCalendarInstant(zoneId) ?: return@mapNotNull null
            val end = properties["DTEND"]?.firstOrNull()?.toCalendarInstant(zoneId)
            val uid = properties["UID"]?.firstOrNull()?.value?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: "$title|${start.epochSecond}"
            val url = properties["URL"]?.firstOrNull()?.value
                ?.unescapeICalendarText()
                ?.let { candidate -> resolveHttpsUrl(content.finalUrl, candidate) }
                ?: content.finalUrl
            buildContest(
                source = source,
                remoteId = uid,
                title = title,
                start = start,
                end = end,
                contestUrl = url,
                fetchedAt = fetchedAt,
            )
        }.toList()
        return buildResult(events, CustomSourceFormat.ICALENDAR, fetchedAt)
    }
}

private fun buildResult(
    candidates: List<Contest>,
    format: CustomSourceFormat,
    fetchedAt: Instant,
): CustomSourceParseResult {
    val upcoming = candidates
        .asSequence()
        .filter { contest ->
            contest.endTime?.isAfter(fetchedAt) ?: !contest.startTime.isBefore(fetchedAt)
        }
        .distinctBy(Contest::id)
        .sortedBy(Contest::startTime)
        .toList()
    val truncated = upcoming.size > MAX_IMPORTED_CONTESTS
    val contests = upcoming.take(MAX_IMPORTED_CONTESTS)
    val warnings = buildList {
        if (contests.any { contest -> contest.endTime == null }) {
            add("部分比赛未提供结束时间")
        }
        if (truncated) add("仅预览并保存最早的 $MAX_IMPORTED_CONTESTS 场比赛")
    }
    return CustomSourceParseResult(contests, format, warnings)
}

private fun JsonElement.collectContestObjects(): List<JsonObject> {
    val result = mutableListOf<JsonObject>()
    fun visit(element: JsonElement) {
        when (element) {
            is JsonArray -> element.forEach(::visit)
            is JsonObject -> {
                if (element.hasContestShape()) result += element
                element.values.forEach(::visit)
            }
            else -> Unit
        }
    }
    visit(this)
    return result
}

private fun JsonObject.hasContestShape(): Boolean =
    findPrimitive(TITLE_KEYS) != null && findPrimitive(START_KEYS) != null

private fun JsonObject.toContest(
    source: CustomContestSource,
    baseUrl: String,
    fetchedAt: Instant,
    zoneId: ZoneId,
    dateTimePattern: String,
    fallbackUrl: String? = null,
): Contest? {
    val title = findPrimitive(TITLE_KEYS)?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val start = findPrimitive(START_KEYS)
        ?.parseFlexibleInstant(zoneId, dateTimePattern)
        ?: return null
    var end = findPrimitive(END_KEYS)?.parseFlexibleInstant(zoneId, dateTimePattern)
    if (end == null) {
        val duration = findPrimitiveWithKey(DURATION_KEYS)?.let { (key, raw) ->
            raw.parseFlexibleDuration(key)
        }
        end = duration?.let { value -> runCatching { start.plus(value) }.getOrNull() }
    }
    if (end != null && !end.isAfter(start)) return null
    val remoteId = findPrimitive(ID_KEYS)?.trim()?.takeIf(String::isNotEmpty)
        ?: "$title|${start.epochSecond}"
    val urlCandidate = findPrimitive(URL_KEYS) ?: fallbackUrl
    val contestUrl = urlCandidate?.let { candidate -> resolveHttpsUrl(baseUrl, candidate) } ?: baseUrl
    return buildContest(source, remoteId, title, start, end, contestUrl, fetchedAt)
}

private fun Element.toEmbeddedJsonContest(
    source: CustomContestSource,
    fetchedAt: Instant,
    zoneId: ZoneId,
    baseUrl: String,
): Contest? {
    val decoded = attr("data-json").decodeNestedHtmlEntities()
    val payload = runCatching { Json.parseToJsonElement(decoded) as? JsonObject }.getOrNull()
        ?: return null
    val link = selectFirst("a[href]")?.absUrl("href")?.takeIf(String::isNotEmpty)
    return payload.toContest(
        source = source,
        baseUrl = baseUrl,
        fetchedAt = fetchedAt,
        zoneId = zoneId,
        dateTimePattern = "",
        fallbackUrl = link,
    )
}

private fun Element.toMappedContest(
    source: CustomContestSource,
    fetchedAt: Instant,
    zoneId: ZoneId,
    baseUrl: String,
): Contest? {
    val selectors = source.selectors
    val title = selectedValue(selectors.title)?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val start = selectedValue(selectors.start)
        ?.parseFlexibleInstant(zoneId, selectors.dateTimePattern)
        ?: return null
    val end = selectors.end.takeIf(String::isNotBlank)
        ?.let(::selectedValue)
        ?.parseFlexibleInstant(zoneId, selectors.dateTimePattern)
    if (end != null && !end.isAfter(start)) return null
    val linkElement = if (selectors.link.isBlank()) selectFirst("a[href]") else selectFirst(selectors.link)
    val contestUrl = linkElement?.absUrl("href")
        ?.takeIf(String::isNotEmpty)
        ?: linkElement?.attr("href")?.let { resolveHttpsUrl(baseUrl, it) }
        ?: baseUrl
    val remoteId = attr("data-id").takeIf(String::isNotBlank)
        ?: attr("id").takeIf(String::isNotBlank)
        ?: contestUrl
    return buildContest(source, remoteId, title, start, end, contestUrl, fetchedAt)
}

private fun org.jsoup.nodes.Document.semanticTimeContests(
    source: CustomContestSource,
    fetchedAt: Instant,
    zoneId: ZoneId,
    baseUrl: String,
): List<Contest> = select("time[datetime]").mapNotNull { timeElement ->
    val row = timeElement.closest("article, li, tr, [class*=contest], [class*=event]")
        ?: timeElement.parent()
        ?: return@mapNotNull null
    val times = row.select("time[datetime]")
    if (times.firstOrNull() != timeElement) return@mapNotNull null
    val titleElement = row.selectFirst("h1, h2, h3, h4, h5, a[href]") ?: return@mapNotNull null
    val title = titleElement.text().trim().takeIf(String::isNotEmpty) ?: return@mapNotNull null
    val start = timeElement.attr("datetime").parseFlexibleInstant(zoneId, "") ?: return@mapNotNull null
    val end = times.getOrNull(1)?.attr("datetime")?.parseFlexibleInstant(zoneId, "")
    val link = row.selectFirst("a[href]")?.absUrl("href")
        ?.takeIf(String::isNotEmpty)
        ?: baseUrl
    buildContest(source, link, title, start, end, link, fetchedAt)
}

private fun Element.selectedValue(selector: String): String? {
    val element = selectFirst(selector) ?: return null
    return listOf("datetime", "content", "data-time", "data-start", "value", "title")
        .firstNotNullOfOrNull { attribute -> element.attr(attribute).takeIf(String::isNotBlank) }
        ?: element.text().takeIf(String::isNotBlank)
}

private fun buildContest(
    source: CustomContestSource,
    remoteId: String,
    title: String,
    start: Instant,
    end: Instant?,
    contestUrl: String,
    fetchedAt: Instant,
): Contest {
    val compactRemoteId = remoteId.trim().take(MAX_REMOTE_ID_LENGTH)
    val namespacedRemoteId = "${source.id}:$compactRemoteId"
    val duration = end?.let { finish ->
        runCatching { Duration.between(start, finish) }.getOrNull()
    }
    return Contest(
        id = "custom:${source.id}:${compactRemoteId.sha256Prefix()}",
        source = ContestSource.OTHER,
        sourceContestId = namespacedRemoteId,
        title = title.trim(),
        startTime = start,
        endTime = end,
        duration = duration,
        registrationUrl = contestUrl,
        contestUrl = contestUrl,
        status = when {
            end != null && !start.isAfter(fetchedAt) && end.isAfter(fetchedAt) -> ContestStatus.RUNNING
            start.isAfter(fetchedAt) -> ContestStatus.UPCOMING
            end == null && start == fetchedAt -> ContestStatus.RUNNING
            else -> ContestStatus.FINISHED
        },
        category = source.name.trim(),
        difficultyLabel = null,
        ratedRange = null,
        isRated = null,
        isFavorite = false,
        reminderOffsets = emptySet(),
        lastUpdatedAt = fetchedAt,
    )
}

private fun JsonObject.findPrimitive(keys: Set<String>): String? = entries.firstNotNullOfOrNull {
    (key, value) ->
    if (key.normalizedKey() in keys) (value as? JsonPrimitive)?.contentOrNull else null
}

private fun JsonObject.findPrimitiveWithKey(keys: Set<String>): Pair<String, String>? =
    entries.firstNotNullOfOrNull { (key, value) ->
        val normalized = key.normalizedKey()
        val content = (value as? JsonPrimitive)?.contentOrNull
        if (normalized in keys && content != null) normalized to content else null
    }

private fun String.normalizedKey(): String = lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)

private fun String.parseFlexibleInstant(zoneId: ZoneId, pattern: String): Instant? {
    val value = trim()
    value.toLongOrNull()?.let { numeric ->
        return runCatching {
            if (kotlin.math.abs(numeric) >= EPOCH_MILLIS_THRESHOLD) {
                Instant.ofEpochMilli(numeric)
            } else {
                Instant.ofEpochSecond(numeric)
            }
        }.getOrNull()
    }
    runCatching { Instant.parse(value) }.getOrNull()?.let { return it }
    runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()?.let { return it }
    runCatching { ZonedDateTime.parse(value).toInstant() }.getOrNull()?.let { return it }
    if (pattern.isNotBlank()) {
        val formatter = runCatching { DateTimeFormatter.ofPattern(pattern, Locale.getDefault()) }
            .getOrNull()
        formatter?.let { configured ->
            runCatching { ZonedDateTime.parse(value, configured).toInstant() }.getOrNull()?.let { return it }
            runCatching { OffsetDateTime.parse(value, configured).toInstant() }.getOrNull()?.let { return it }
            runCatching { LocalDateTime.parse(value, configured).atZone(zoneId).toInstant() }
                .getOrNull()
                ?.let { return it }
        }
    }
    COMMON_LOCAL_DATE_TIME_FORMATTERS.forEach { formatter ->
        runCatching { LocalDateTime.parse(value, formatter).atZone(zoneId).toInstant() }
            .getOrNull()
            ?.let { return it }
    }
    return runCatching { LocalDate.parse(value).atStartOfDay(zoneId).toInstant() }.getOrNull()
}

private fun String.parseFlexibleDuration(normalizedKey: String): Duration? {
    val numeric = trim().toLongOrNull() ?: return null
    if (numeric <= 0L) return null
    return runCatching {
        when {
            "millis" in normalizedKey || numeric > MAX_REASONABLE_DURATION_SECONDS -> {
                Duration.ofMillis(numeric)
            }
            "minute" in normalizedKey -> Duration.ofMinutes(numeric)
            else -> Duration.ofSeconds(numeric)
        }
    }.getOrNull()
}

private fun String.decodeNestedHtmlEntities(): String {
    var decoded = this
    repeat(MAX_ENTITY_DECODE_PASSES) {
        val next = Parser.unescapeEntities(decoded, true)
        if (next == decoded) return decoded
        decoded = next
    }
    return decoded
}

private data class CalendarProperty(
    val name: String,
    val parameters: Map<String, String>,
    val value: String,
) {
    fun toCalendarInstant(defaultZoneId: ZoneId): Instant? {
        val timezone = parameters["TZID"]?.let { id -> runCatching { ZoneId.of(id) }.getOrNull() }
            ?: defaultZoneId
        val raw = value.trim()
        return when {
            raw.endsWith("Z") -> runCatching {
                LocalDateTime.parse(raw.dropLast(1), ICALENDAR_DATE_TIME_FORMATTER)
                    .atZone(ZoneId.of("UTC"))
                    .toInstant()
            }.getOrNull()
            raw.length == ICALENDAR_DATE_LENGTH -> runCatching {
                LocalDate.parse(raw, ICALENDAR_DATE_FORMATTER).atStartOfDay(timezone).toInstant()
            }.getOrNull()
            else -> runCatching {
                LocalDateTime.parse(raw, ICALENDAR_DATE_TIME_FORMATTER).atZone(timezone).toInstant()
            }.getOrNull()
        }
    }
}

private fun parseCalendarProperty(line: String): CalendarProperty? {
    val separator = line.indexOf(':')
    if (separator <= 0) return null
    val declaration = line.substring(0, separator)
    val parts = declaration.split(';')
    val name = parts.first().uppercase(Locale.ROOT)
    val parameters = parts.drop(1).mapNotNull { parameter ->
        val equals = parameter.indexOf('=')
        if (equals <= 0) null else parameter.substring(0, equals).uppercase(Locale.ROOT) to
            parameter.substring(equals + 1).trim('"')
    }.toMap()
    return CalendarProperty(name, parameters, line.substring(separator + 1))
}

private fun String.unescapeICalendarText(): String = replace("\\n", "\n", ignoreCase = true)
    .replace("\\,", ",")
    .replace("\\;", ";")
    .replace("\\\\", "\\")

private fun resolveHttpsUrl(baseUrl: String, candidate: String): String {
    val resolved = runCatching { URI(baseUrl).resolve(candidate.trim()) }.getOrNull()
    return if (resolved?.scheme.equals("https", ignoreCase = true) && resolved?.host != null) {
        resolved.toString()
    } else {
        baseUrl
    }
}

private fun String.sha256Prefix(): String = MessageDigest.getInstance("SHA-256")
    .digest(toByteArray())
    .take(HASH_BYTES)
    .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

private val TITLE_KEYS = setOf("title", "name", "contestname", "eventname", "summary")
private val START_KEYS = setOf(
    "start", "starttime", "startat", "begintime", "beginat", "conteststarttime", "startdate",
)
private val END_KEYS = setOf(
    "end", "endtime", "endat", "finishtime", "finishat", "contestendtime", "enddate",
)
private val ID_KEYS = setOf("id", "contestid", "eventid", "uid", "key")
private val URL_KEYS = setOf("url", "contesturl", "eventurl", "link", "href", "registrationurl")
private val DURATION_KEYS = setOf(
    "duration", "contestduration", "durationseconds", "durationminutes", "durationmillis",
)
private val COMMON_LOCAL_DATE_TIME_FORMATTERS = listOf(
    "yyyy-MM-dd HH:mm:ss",
    "yyyy-MM-dd HH:mm",
    "yyyy/MM/dd HH:mm:ss",
    "yyyy/MM/dd HH:mm",
    "yyyy年M月d日 HH:mm",
).map(DateTimeFormatter::ofPattern)
private val ICALENDAR_EVENT_PATTERN = Regex(
    "BEGIN:VEVENT\\n(.*?)\\nEND:VEVENT",
    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
)
private val ICALENDAR_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
private val ICALENDAR_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE
private const val ICALENDAR_DATE_LENGTH = 8
private const val MAX_IMPORTED_CONTESTS = 200
private const val MAX_REMOTE_ID_LENGTH = 160
private const val HASH_BYTES = 12
private const val MAX_ENTITY_DECODE_PASSES = 3
private const val EPOCH_MILLIS_THRESHOLD = 100_000_000_000L
private const val MAX_REASONABLE_DURATION_SECONDS = 604_800L
