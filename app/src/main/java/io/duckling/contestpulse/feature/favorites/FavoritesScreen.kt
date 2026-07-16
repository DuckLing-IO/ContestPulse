package io.duckling.contestpulse.feature.favorites

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.duckling.contestpulse.R
import io.duckling.contestpulse.core.designsystem.component.FadeTransition
import io.duckling.contestpulse.core.designsystem.component.topLevelScreenPadding
import io.duckling.contestpulse.core.designsystem.theme.PulseTheme
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.feature.common.CalendarBackAction
import io.duckling.contestpulse.feature.common.ContestCard
import io.duckling.contestpulse.feature.common.ContestCalendar
import io.duckling.contestpulse.feature.common.ContestDisplayMode
import io.duckling.contestpulse.feature.common.CalendarModeToggle
import io.duckling.contestpulse.feature.common.EmptyState
import io.duckling.contestpulse.feature.common.LoadingCards
import io.duckling.contestpulse.feature.common.PageHeader
import io.duckling.contestpulse.feature.common.SelectableChip
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FavoritesRoute(
    onContestClick: (String) -> Unit,
    sharedElementModifier: @Composable (Contest) -> Modifier,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FavoritesScreen(
        uiState = uiState,
        onSelectSegment = viewModel::selectSegment,
        onContestClick = onContestClick,
        onToggleFavorite = viewModel::toggleFavorite,
        onToggleDisplayMode = viewModel::toggleDisplayMode,
        onSelectCalendarDate = viewModel::selectCalendarDate,
        onReturnToCalendar = viewModel::returnToCalendar,
        onShowPreviousCalendarMonth = viewModel::showPreviousCalendarMonth,
        onShowNextCalendarMonth = viewModel::showNextCalendarMonth,
        onSelectCalendarMonth = viewModel::selectCalendarMonth,
        sharedElementModifier = sharedElementModifier,
    )
}

@Composable
fun FavoritesScreen(
    uiState: FavoritesUiState,
    onSelectSegment: (FavoriteSegment) -> Unit,
    onContestClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    sharedElementModifier: @Composable (Contest) -> Modifier,
    modifier: Modifier = Modifier,
    onToggleDisplayMode: () -> Unit = {},
    onSelectCalendarDate: (LocalDate) -> Unit = {},
    onReturnToCalendar: () -> Unit = {},
    onShowPreviousCalendarMonth: () -> Unit = {},
    onShowNextCalendarMonth: () -> Unit = {},
    onSelectCalendarMonth: (YearMonth) -> Unit = {},
) {
    BackHandler(
        enabled = uiState.displayMode == ContestDisplayMode.CALENDAR &&
            uiState.selectedCalendarDate != null,
        onBack = onReturnToCalendar,
    )
    val zoneId = uiState.zoneId
    val selectedDateLabel = uiState.selectedCalendarDate?.format(CALENDAR_DAY_TITLE_FORMATTER)
    val headerTitle = if (selectedDateLabel == null) {
        stringResource(R.string.favorites_title)
    } else {
        stringResource(R.string.calendar_day_title, selectedDateLabel)
    }
    val selectedDateContests = uiState.selectedCalendarDate?.let { date ->
        uiState.calendarContests.filter { contest ->
            contest.startTime.atZone(zoneId).toLocalDate() == date
        }
    }.orEmpty()
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        FadeTransition(
            visible = uiState.isLoading,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = topLevelScreenPadding(),
            ) {
                item(key = "favorites-loading-header") {
                    PageHeader(
                        eyebrow = stringResource(R.string.favorites_eyebrow),
                        title = stringResource(R.string.favorites_title),
                        subtitle = stringResource(R.string.favorites_subtitle),
                    )
                    Spacer(modifier = Modifier.height(PulseTheme.spacing.xxl))
                }
                item(key = "favorites-loading-cards") { LoadingCards() }
            }
        }
        FadeTransition(
            visible = !uiState.isLoading,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = topLevelScreenPadding(),
            ) {
                item(key = "favorites-header") {
                    PageHeader(
                        eyebrow = stringResource(R.string.favorites_eyebrow),
                        title = headerTitle,
                        subtitle = stringResource(
                            if (uiState.selectedCalendarDate == null) {
                                R.string.favorites_subtitle
                            } else {
                                R.string.calendar_day_subtitle
                            },
                        ),
                        trailingContent = {
                            if (uiState.selectedCalendarDate != null) {
                                CalendarBackAction(onClick = onReturnToCalendar)
                            } else {
                                CalendarModeToggle(
                                    displayMode = uiState.displayMode,
                                    onClick = onToggleDisplayMode,
                                )
                            }
                        },
                    )
                    if (uiState.displayMode == ContestDisplayMode.LIST) {
                        Spacer(modifier = Modifier.height(PulseTheme.spacing.xl))
                        FavoriteSegments(
                            selected = uiState.selectedSegment,
                            onSelect = onSelectSegment,
                        )
                    }
                    Spacer(modifier = Modifier.height(PulseTheme.spacing.xxl))
                }

                when {
                    uiState.selectedCalendarDate != null -> {
                        items(
                            items = selectedDateContests,
                            key = Contest::id,
                        ) { contest ->
                            ContestCard(
                                contest = contest,
                                now = uiState.now,
                                zoneId = zoneId,
                                onClick = { onContestClick(contest.id) },
                                onToggleFavorite = { onToggleFavorite(contest.id) },
                                modifier = sharedElementModifier(contest),
                            )
                            Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
                        }
                        item(key = "favorites-calendar-day-empty") {
                            FadeTransition(visible = selectedDateContests.isEmpty()) {
                                EmptyState(
                                    title = stringResource(R.string.calendar_empty_title),
                                    body = stringResource(R.string.calendar_empty_body),
                                )
                            }
                        }
                    }
                    uiState.displayMode == ContestDisplayMode.CALENDAR -> {
                        item(key = "favorites-calendar") {
                            ContestCalendar(
                                contests = uiState.calendarContests,
                                month = uiState.calendarMonth,
                                now = uiState.now,
                                zoneId = zoneId,
                                onPreviousMonth = onShowPreviousCalendarMonth,
                                onNextMonth = onShowNextCalendarMonth,
                                onMonthSelected = onSelectCalendarMonth,
                                onDateClick = onSelectCalendarDate,
                            )
                        }
                        item(key = "favorites-calendar-empty") {
                            FadeTransition(
                                visible = uiState.calendarContests.none { contest ->
                                    java.time.YearMonth.from(contest.startTime.atZone(zoneId)) ==
                                        uiState.calendarMonth
                                },
                            ) {
                                EmptyState(
                                    title = stringResource(R.string.calendar_empty_title),
                                    body = stringResource(R.string.calendar_empty_body),
                                )
                            }
                        }
                    }
                    else -> {
                        items(
                            items = uiState.contests,
                            key = Contest::id,
                        ) { contest ->
                            ContestCard(
                                contest = contest,
                                now = uiState.now,
                                zoneId = zoneId,
                                onClick = { onContestClick(contest.id) },
                                onToggleFavorite = { onToggleFavorite(contest.id) },
                                modifier = sharedElementModifier(contest),
                            )
                            Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
                        }

                        item(key = "favorites-empty") {
                            FadeTransition(visible = uiState.contests.isEmpty()) {
                                EmptyState(
                                    title = stringResource(R.string.favorites_empty_title),
                                    body = stringResource(R.string.favorites_empty_body),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteSegments(
    selected: FavoriteSegment,
    onSelect: (FavoriteSegment) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.xs),
    ) {
        FavoriteSegment.entries.forEach { segment ->
            SelectableChip(
                label = stringResource(segment.labelRes),
                selected = selected == segment,
                onClick = { onSelect(segment) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val FavoriteSegment.labelRes: Int
    get() = when (this) {
        FavoriteSegment.UPCOMING -> R.string.favorites_segment_upcoming
        FavoriteSegment.FINISHED -> R.string.favorites_segment_finished
        FavoriteSegment.ALL -> R.string.favorites_segment_all
    }

private val CALENDAR_DAY_TITLE_FORMATTER = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
