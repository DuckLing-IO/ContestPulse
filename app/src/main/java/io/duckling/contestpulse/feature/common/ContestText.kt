package io.duckling.contestpulse.feature.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import io.duckling.contestpulse.R
import io.duckling.contestpulse.domain.logic.countdownAt
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestCountdown
import io.duckling.contestpulse.domain.model.ContestGroupType
import io.duckling.contestpulse.domain.model.ContestSource
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ContestSource.label(): String = stringResource(labelRes)

@Composable
fun Contest.platformLabel(): String = if (source == ContestSource.OTHER) {
    category?.takeIf(String::isNotBlank) ?: source.label()
} else {
    source.label()
}

@get:StringRes
private val ContestSource.labelRes: Int
    get() = when (this) {
        ContestSource.CODEFORCES -> R.string.source_codeforces
        ContestSource.ATCODER -> R.string.source_atcoder
        ContestSource.LUOGU -> R.string.source_luogu
        ContestSource.NOWCODER -> R.string.source_nowcoder
        ContestSource.OTHER -> R.string.source_other
    }

@Composable
fun ContestGroupType.label(): String = stringResource(
    when (this) {
        ContestGroupType.RUNNING -> R.string.contest_group_running
        ContestGroupType.TODAY -> R.string.contest_group_today
        ContestGroupType.TOMORROW -> R.string.contest_group_tomorrow
        ContestGroupType.THIS_WEEK -> R.string.contest_group_this_week
        ContestGroupType.LATER -> R.string.contest_group_later
    },
)

@Composable
fun Contest.countdownLabel(
    now: Instant,
    zoneId: ZoneId,
): String = when (val countdown = countdownAt(now, zoneId)) {
    ContestCountdown.Running -> stringResource(R.string.contest_countdown_running)
    ContestCountdown.Finished -> stringResource(R.string.contest_countdown_finished)
    ContestCountdown.Unknown -> stringResource(R.string.contest_countdown_unknown)
    is ContestCountdown.Days -> stringResource(
        R.string.contest_countdown_days,
        countdown.value,
    )

    is ContestCountdown.HoursMinutes -> if (countdown.hours == 0L) {
        stringResource(R.string.contest_countdown_minutes, countdown.minutes)
    } else {
        stringResource(
            R.string.contest_countdown_hours_minutes,
            countdown.hours,
            countdown.minutes,
        )
    }
}

@Composable
fun Instant.localDateTimeLabel(
    @StringRes patternRes: Int = R.string.contest_date_pattern,
): String {
    val pattern = stringResource(patternRes)
    val locale = Locale.getDefault()
    val zoneId = ZoneId.systemDefault()
    val formatter = remember(pattern, locale, zoneId) {
        DateTimeFormatter.ofPattern(pattern, locale).withZone(zoneId)
    }
    return formatter.format(this)
}

@Composable
fun Duration?.durationLabel(): String {
    if (this == null) return stringResource(R.string.contest_duration_unknown)
    val totalMinutes = toMinutes()
    val hours = totalMinutes / MINUTES_PER_HOUR
    val minutes = totalMinutes % MINUTES_PER_HOUR
    return if (hours == 0L) {
        stringResource(R.string.contest_duration_minutes, minutes)
    } else {
        stringResource(R.string.contest_duration_hours_minutes, hours, minutes)
    }
}

private const val MINUTES_PER_HOUR = 60L
