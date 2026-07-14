package io.duckling.contestpulse.domain.customsource

import java.net.URI
import java.time.ZoneId
import kotlinx.serialization.Serializable

@Serializable
data class CustomContestSource(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    val format: CustomSourceFormat = CustomSourceFormat.AUTO,
    val timezoneId: String = DEFAULT_CUSTOM_SOURCE_TIMEZONE,
    val selectors: CustomHtmlSelectors = CustomHtmlSelectors(),
) {
    val sourceKey: String
        get() = "$CUSTOM_SOURCE_KEY_PREFIX$id"

    fun requireValid() {
        require(id.matches(SOURCE_ID_PATTERN)) { "Invalid custom source id" }
        require(name.trim().length in 1..MAX_CUSTOM_SOURCE_NAME_LENGTH) {
            "Source name must contain 1-$MAX_CUSTOM_SOURCE_NAME_LENGTH characters"
        }
        require(url.length <= MAX_CUSTOM_SOURCE_URL_LENGTH) { "Source URL is too long" }
        val uri = runCatching { URI(url.trim()) }.getOrNull()
        require(uri?.scheme.equals("https", ignoreCase = true)) { "Only HTTPS URLs are supported" }
        require(!uri?.host.isNullOrBlank()) { "Source URL must include a host" }
        require(uri?.userInfo == null) { "Source URL must not contain credentials" }
        runCatching { ZoneId.of(timezoneId) }
            .getOrElse { throw IllegalArgumentException("Invalid timezone id", it) }
        if (format == CustomSourceFormat.HTML || selectors.hasAnyValue) {
            selectors.requireValid()
        }
    }
}

@Serializable
enum class CustomSourceFormat {
    AUTO,
    JSON,
    ICALENDAR,
    HTML,
}

@Serializable
data class CustomHtmlSelectors(
    val item: String = "",
    val title: String = "",
    val start: String = "",
    val end: String = "",
    val link: String = "",
    val dateTimePattern: String = "",
) {
    val hasAnyValue: Boolean
        get() = listOf(item, title, start, end, link, dateTimePattern).any(String::isNotBlank)

    fun requireValid() {
        require(item.isNotBlank()) { "Contest item selector is required" }
        require(title.isNotBlank()) { "Title selector is required" }
        require(start.isNotBlank()) { "Start-time selector is required" }
        listOf(item, title, start, end, link, dateTimePattern).forEach { value ->
            require(value.length <= MAX_SELECTOR_LENGTH) { "A selector or pattern is too long" }
        }
    }
}

const val MAX_CUSTOM_SOURCES = 10
const val CUSTOM_SOURCE_KEY_PREFIX = "custom:"
const val DEFAULT_CUSTOM_SOURCE_TIMEZONE = "Asia/Shanghai"
private const val MAX_CUSTOM_SOURCE_NAME_LENGTH = 32
private const val MAX_CUSTOM_SOURCE_URL_LENGTH = 2_048
private const val MAX_SELECTOR_LENGTH = 256
private val SOURCE_ID_PATTERN = Regex("[a-zA-Z0-9-]{8,64}")
