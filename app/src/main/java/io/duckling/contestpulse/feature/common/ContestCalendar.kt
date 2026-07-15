package io.duckling.contestpulse.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.duckling.contestpulse.R
import io.duckling.contestpulse.core.designsystem.component.appCard
import io.duckling.contestpulse.core.designsystem.component.pressEffect
import io.duckling.contestpulse.core.designsystem.theme.PulseTheme
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestSource
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ContestDisplayMode {
    LIST,
    CALENDAR,
}

@Composable
fun CalendarModeToggle(
    displayMode: ContestDisplayMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCalendar = displayMode == ContestDisplayMode.CALENDAR
    val label = if (isCalendar) {
        stringResource(R.string.calendar_show_list)
    } else {
        stringResource(R.string.calendar_show_calendar)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                color = PulseTheme.colors.surfaceMuted,
                shape = RoundedCornerShape(PulseTheme.radius.full),
            )
            .pressEffect(
                contentDescription = label,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = PulseTheme.spacing.md),
    ) {
        Text(
            text = label,
            color = PulseTheme.colors.textPrimary,
            style = PulseTheme.typography.footnote,
        )
    }
}

@Composable
fun CalendarBackAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.calendar_back)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                color = PulseTheme.colors.surfaceMuted,
                shape = RoundedCornerShape(PulseTheme.radius.full),
            )
            .pressEffect(
                contentDescription = label,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = PulseTheme.spacing.md),
    ) {
        Text(
            text = label,
            color = PulseTheme.colors.textPrimary,
            style = PulseTheme.typography.footnote,
        )
    }
}

@Composable
fun ContestCalendar(
    contests: List<Contest>,
    month: YearMonth,
    now: Instant,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val currentDate = now.atZone(zoneId).toLocalDate()
    val contestsByDate = remember(contests, zoneId) {
        contests.groupBy { contest -> contest.startTime.atZone(zoneId).toLocalDate() }
    }
    val cells = remember(month) { month.calendarCells() }
    val weekdayLabels = listOf(
        stringResource(R.string.calendar_weekday_mon),
        stringResource(R.string.calendar_weekday_tue),
        stringResource(R.string.calendar_weekday_wed),
        stringResource(R.string.calendar_weekday_thu),
        stringResource(R.string.calendar_weekday_fri),
        stringResource(R.string.calendar_weekday_sat),
        stringResource(R.string.calendar_weekday_sun),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .appCard()
            .padding(PulseTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalendarMonthControl(
                label = stringResource(R.string.calendar_previous_month),
                symbol = "‹",
                onClick = onPreviousMonth,
            )
            Text(
                text = month.format(CALENDAR_MONTH_FORMATTER),
                color = PulseTheme.colors.textPrimary,
                style = PulseTheme.typography.title3,
            )
            CalendarMonthControl(
                label = stringResource(R.string.calendar_next_month),
                symbol = "›",
                onClick = onNextMonth,
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    color = PulseTheme.colors.textTertiary,
                    style = PulseTheme.typography.caption1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        cells.chunked(DAYS_PER_WEEK).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.xxs),
            ) {
                week.forEach { date ->
                    CalendarDay(
                        date = date,
                        contests = date?.let(contestsByDate::get).orEmpty(),
                        isToday = date == currentDate,
                        onClick = { date?.let(onDateClick) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthControl(
    label: String,
    symbol: String,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(PulseTheme.spacing.huge)
            .background(
                color = PulseTheme.colors.surface,
                shape = RoundedCornerShape(PulseTheme.radius.full),
            )
            .pressEffect(
                contentDescription = label,
                onClick = onClick,
            ),
    ) {
        Text(
            text = symbol,
            color = PulseTheme.colors.textPrimary,
            style = PulseTheme.typography.title2,
        )
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate?,
    contests: List<Contest>,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasContests = contests.isNotEmpty()
    val description = date?.let { currentDate ->
        if (hasContests) {
            stringResource(R.string.calendar_open_date, currentDate.format(CALENDAR_DAY_FORMATTER))
        } else {
            currentDate.format(CALENDAR_DAY_FORMATTER)
        }
    }.orEmpty()
    val dayModifier = modifier
        .height(PulseTheme.spacing.giant + PulseTheme.spacing.xs)
        .background(
            color = when {
                hasContests -> PulseTheme.colors.surfaceMuted
                isToday -> PulseTheme.colors.separator
                else -> PulseTheme.colors.surface
            },
            shape = RoundedCornerShape(PulseTheme.radius.sm),
        )
    val interactiveModifier = if (hasContests) {
        dayModifier.pressEffect(
            contentDescription = description,
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        dayModifier
    }

    Column(
        modifier = interactiveModifier.padding(PulseTheme.spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = date?.dayOfMonth?.toString().orEmpty(),
            color = if (isToday) PulseTheme.colors.textPrimary else PulseTheme.colors.textSecondary,
            style = PulseTheme.typography.footnote.copy(
                fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
            ),
            textAlign = TextAlign.Center,
        )
        if (hasContests) {
            Spacer(modifier = Modifier.height(PulseTheme.spacing.xxs))
            Text(
                text = contests.platformSummary(),
                color = PulseTheme.colors.textPrimary,
                style = PulseTheme.typography.caption2,
                textAlign = TextAlign.Center,
                maxLines = MAX_PLATFORM_LINES,
            )
        }
    }
}

private fun YearMonth.calendarCells(): List<LocalDate?> {
    val firstDayOffset = atDay(1).dayOfWeek.value - 1
    val dates = List(firstDayOffset) { null } +
        (1..lengthOfMonth()).map(::atDay)
    val trailingDays = (DAYS_PER_WEEK - dates.size % DAYS_PER_WEEK) % DAYS_PER_WEEK
    return dates + List(trailingDays) { null }
}

private fun List<Contest>.platformSummary(): String {
    val platforms = asSequence()
        .map(Contest::source)
        .distinct()
        .toList()
    val visible = platforms.take(MAX_VISIBLE_PLATFORMS).joinToString("·") { source ->
        source.calendarLabel()
    }
    return if (platforms.size > MAX_VISIBLE_PLATFORMS) "$visible+" else visible
}

private fun ContestSource.calendarLabel(): String = when (this) {
    ContestSource.CODEFORCES -> "CF"
    ContestSource.ATCODER -> "AC"
    ContestSource.LUOGU -> "洛谷"
    ContestSource.NOWCODER -> "牛客"
    ContestSource.OTHER -> "其他"
}

private val CALENDAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)
private val CALENDAR_DAY_FORMATTER = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
private const val DAYS_PER_WEEK = 7
private const val MAX_VISIBLE_PLATFORMS = 2
private const val MAX_PLATFORM_LINES = 2
