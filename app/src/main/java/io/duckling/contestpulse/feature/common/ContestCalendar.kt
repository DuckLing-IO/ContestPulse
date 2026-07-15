package io.duckling.contestpulse.feature.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.abs

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
    val label = stringResource(
        if (isCalendar) R.string.calendar_show_list else R.string.calendar_show_calendar,
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                color = PulseTheme.colors.surfaceMuted,
                shape = RoundedCornerShape(PulseTheme.radius.full),
            )
            .pressEffect(contentDescription = label, role = Role.Button, onClick = onClick)
            .padding(horizontal = PulseTheme.spacing.md),
    ) {
        Text(text = label, color = PulseTheme.colors.textPrimary, style = PulseTheme.typography.footnote)
    }
}

@Composable
fun CalendarBackAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.calendar_back)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                color = PulseTheme.colors.surfaceMuted,
                shape = RoundedCornerShape(PulseTheme.radius.full),
            )
            .pressEffect(contentDescription = label, role = Role.Button, onClick = onClick)
            .padding(horizontal = PulseTheme.spacing.md),
    ) {
        Text(text = label, color = PulseTheme.colors.textPrimary, style = PulseTheme.typography.footnote)
    }
}

@Composable
fun ContestCalendar(
    contests: List<Contest>,
    month: YearMonth,
    now: Instant,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthSelected: (YearMonth) -> Unit,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val currentDate = now.atZone(zoneId).toLocalDate()
    val contestsByDate = remember(contests, zoneId) {
        contests.groupBy { contest -> contest.startTime.atZone(zoneId).toLocalDate() }
    }
    var isYearPickerVisible by rememberSaveable { mutableStateOf(false) }
    var displayedYear by rememberSaveable { mutableIntStateOf(month.year) }
    LaunchedEffect(month.year, isYearPickerVisible) {
        if (!isYearPickerVisible) displayedYear = month.year
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .appCard()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy, // settles month/year height changes cleanly
                    stiffness = Spring.StiffnessMedium, // keeps the calendar responsive
                ),
            )
            .padding(PulseTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.md),
    ) {
        CalendarHeader(
            month = month,
            displayedYear = displayedYear,
            isYearPickerVisible = isYearPickerVisible,
            onTitleClick = {
                displayedYear = month.year
                isYearPickerVisible = !isYearPickerVisible
            },
            onPrevious = {
                if (isYearPickerVisible) displayedYear-- else onPreviousMonth()
            },
            onNext = {
                if (isYearPickerVisible) displayedYear++ else onNextMonth()
            },
        )
        AnimatedContent(
            targetState = isYearPickerVisible,
            transitionSpec = {
                (fadeIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy, // avoids a jarring panel appearance
                        stiffness = Spring.StiffnessMedium, // moderate reveal speed
                    ),
                ) + slideInHorizontally(
                    initialOffsetX = { it / 8 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy, // smooth spatial transition
                        stiffness = Spring.StiffnessLow, // gentle large-panel movement
                    ),
                )).togetherWith(
                    fadeOut(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy, // clean outgoing fade
                            stiffness = Spring.StiffnessMedium, // responsive transition
                        ),
                    ) + slideOutHorizontally(
                        targetOffsetX = { -it / 8 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy, // no overshoot on exit
                            stiffness = Spring.StiffnessLow, // gentle large-panel movement
                        ),
                    ),
                )
            },
            label = "calendarMode",
        ) { showYearPicker ->
            if (showYearPicker) {
                CalendarYearPicker(
                    year = displayedYear,
                    selectedMonth = month,
                    onPreviousYear = { displayedYear-- },
                    onNextYear = { displayedYear++ },
                    onMonthClick = { monthNumber ->
                        onMonthSelected(YearMonth.of(displayedYear, monthNumber))
                        isYearPickerVisible = false
                    },
                )
            } else {
                CalendarMonthGrid(
                    month = month,
                    contestsByDate = contestsByDate,
                    currentDate = currentDate,
                    onDateClick = onDateClick,
                )
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    month: YearMonth,
    displayedYear: Int,
    isYearPickerVisible: Boolean,
    onTitleClick: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val previousLabel = stringResource(
        if (isYearPickerVisible) R.string.calendar_previous_year else R.string.calendar_previous_month,
    )
    val nextLabel = stringResource(
        if (isYearPickerVisible) R.string.calendar_next_year else R.string.calendar_next_month,
    )
    val title = if (isYearPickerVisible) {
        stringResource(R.string.calendar_year_title, displayedYear)
    } else {
        month.format(CALENDAR_MONTH_FORMATTER)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarMonthControl(label = previousLabel, symbol = "‹", onClick = onPrevious)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .pressEffect(
                    contentDescription = stringResource(R.string.calendar_choose_month),
                    onClick = onTitleClick,
                )
                .padding(horizontal = PulseTheme.spacing.md),
        ) {
            Text(text = title, color = PulseTheme.colors.textPrimary, style = PulseTheme.typography.title3)
        }
        CalendarMonthControl(label = nextLabel, symbol = "›", onClick = onNext)
    }
}

@Composable
private fun CalendarMonthGrid(
    month: YearMonth,
    contestsByDate: Map<LocalDate, List<Contest>>,
    currentDate: LocalDate,
    onDateClick: (LocalDate) -> Unit,
) {
    val weekdayLabels = listOf(
        stringResource(R.string.calendar_weekday_mon),
        stringResource(R.string.calendar_weekday_tue),
        stringResource(R.string.calendar_weekday_wed),
        stringResource(R.string.calendar_weekday_thu),
        stringResource(R.string.calendar_weekday_fri),
        stringResource(R.string.calendar_weekday_sat),
        stringResource(R.string.calendar_weekday_sun),
    )
    AnimatedContent(
        targetState = month,
        transitionSpec = {
            val direction = if (targetState >= initialState) 1 else -1
            (slideInHorizontally(
                initialOffsetX = { direction * it / 5 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy, // smooth month movement without bounce
                    stiffness = Spring.StiffnessLow, // relaxed page-sized transition
                ),
            ) + fadeIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy, // keeps text legible during motion
                    stiffness = Spring.StiffnessMedium, // balanced fade speed
                ),
            )).togetherWith(
                slideOutHorizontally(
                    targetOffsetX = { -direction * it / 5 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy, // smooth outgoing month
                        stiffness = Spring.StiffnessLow, // relaxed page-sized transition
                    ),
                ) + fadeOut(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy, // clean outgoing fade
                        stiffness = Spring.StiffnessMedium, // balanced fade speed
                    ),
                ),
            )
        },
        label = "calendarMonth",
    ) { visibleMonth ->
        Column(verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.md)) {
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
            visibleMonth.calendarCells().chunked(DAYS_PER_WEEK).forEach { week ->
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
}

@Composable
private fun CalendarYearPicker(
    year: Int,
    selectedMonth: YearMonth,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onMonthClick: (Int) -> Unit,
) {
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = with(LocalDensity.current) { PulseTheme.spacing.huge.toPx() }
    AnimatedContent(
        targetState = year,
        modifier = Modifier.pointerInput(year) {
            detectHorizontalDragGestures(
                onDragStart = { dragDistance = 0f },
                onHorizontalDrag = { change, amount ->
                    change.consume()
                    dragDistance += amount
                },
                onDragEnd = {
                    if (abs(dragDistance) >= swipeThresholdPx) {
                        if (dragDistance < 0f) onNextYear() else onPreviousYear()
                    }
                    dragDistance = 0f
                },
                onDragCancel = { dragDistance = 0f },
            )
        },
        transitionSpec = {
            val direction = if (targetState >= initialState) 1 else -1
            (slideInHorizontally(
                initialOffsetX = { direction * it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy, // follows a year swipe without overshoot
                    stiffness = Spring.StiffnessLow, // natural full-width travel
                ),
            ) + fadeIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy, // smooth year content reveal
                    stiffness = Spring.StiffnessMedium, // responsive text transition
                ),
            )).togetherWith(
                slideOutHorizontally(
                    targetOffsetX = { -direction * it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy, // follows the swipe direction cleanly
                        stiffness = Spring.StiffnessLow, // natural full-width travel
                    ),
                ) + fadeOut(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy, // clean outgoing year fade
                        stiffness = Spring.StiffnessMedium, // responsive text transition
                    ),
                ),
            )
        },
        label = "calendarYear",
    ) { visibleYear ->
        Column(verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.sm)) {
            (1..12).chunked(MONTH_COLUMNS).forEach { rowMonths ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.sm),
                ) {
                    rowMonths.forEach { monthNumber ->
                        val selected = selectedMonth.year == visibleYear &&
                            selectedMonth.monthValue == monthNumber
                        val label = stringResource(R.string.calendar_month_option, monthNumber)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(PulseTheme.spacing.giant)
                                .background(
                                    color = if (selected) PulseTheme.colors.accent else PulseTheme.colors.surface,
                                    shape = RoundedCornerShape(PulseTheme.radius.md),
                                )
                                .pressEffect(
                                    contentDescription = stringResource(
                                        R.string.calendar_select_month,
                                        visibleYear,
                                        monthNumber,
                                    ),
                                    onClick = { onMonthClick(monthNumber) },
                                ),
                        ) {
                            Text(
                                text = label,
                                color = if (selected) PulseTheme.colors.onAccent else PulseTheme.colors.textPrimary,
                                style = PulseTheme.typography.headline,
                            )
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.calendar_year_swipe_hint),
                color = PulseTheme.colors.textTertiary,
                style = PulseTheme.typography.caption1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CalendarMonthControl(label: String, symbol: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(PulseTheme.spacing.huge)
            .background(color = PulseTheme.colors.surface, shape = RoundedCornerShape(PulseTheme.radius.full))
            .pressEffect(contentDescription = label, onClick = onClick),
    ) {
        Text(text = symbol, color = PulseTheme.colors.textPrimary, style = PulseTheme.typography.title2)
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
                isToday -> PulseTheme.colors.accent
                hasContests -> PulseTheme.colors.separator
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
    val contentColor = if (isToday) PulseTheme.colors.onAccent else PulseTheme.colors.textPrimary

    Column(
        modifier = interactiveModifier.padding(PulseTheme.spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = date?.dayOfMonth?.toString().orEmpty(),
            color = if (isToday) contentColor else PulseTheme.colors.textSecondary,
            style = PulseTheme.typography.footnote.copy(
                fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
            ),
            textAlign = TextAlign.Center,
        )
        if (hasContests) {
            Spacer(modifier = Modifier.height(PulseTheme.spacing.xxs))
            Text(
                text = contests.platformSummary(),
                color = contentColor,
                style = PulseTheme.typography.caption2,
                textAlign = TextAlign.Center,
                maxLines = MAX_PLATFORM_LINES,
            )
        }
    }
}

private fun YearMonth.calendarCells(): List<LocalDate?> {
    val firstDayOffset = atDay(1).dayOfWeek.value - 1
    val dates = List(firstDayOffset) { null } + (1..lengthOfMonth()).map(::atDay)
    val trailingDays = (DAYS_PER_WEEK - dates.size % DAYS_PER_WEEK) % DAYS_PER_WEEK
    return dates + List(trailingDays) { null }
}

private fun List<Contest>.platformSummary(): String {
    val platforms = asSequence().map(Contest::source).distinct().toList()
    val visible = platforms.take(MAX_VISIBLE_PLATFORMS).joinToString("·") { it.calendarLabel() }
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
private const val MONTH_COLUMNS = 3
private const val MAX_VISIBLE_PLATFORMS = 2
private const val MAX_PLATFORM_LINES = 2
