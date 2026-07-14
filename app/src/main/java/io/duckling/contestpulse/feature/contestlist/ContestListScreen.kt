package io.duckling.contestpulse.feature.contestlist

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.duckling.contestpulse.R
import io.duckling.contestpulse.core.designsystem.component.FadeTransition
import io.duckling.contestpulse.core.designsystem.component.pressEffect
import io.duckling.contestpulse.core.designsystem.component.topLevelScreenPadding
import io.duckling.contestpulse.core.designsystem.theme.PulseTheme
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestDateRange
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.model.SyncErrorType
import io.duckling.contestpulse.feature.common.ContestCard
import io.duckling.contestpulse.feature.common.EmptyState
import io.duckling.contestpulse.feature.common.LoadingCards
import io.duckling.contestpulse.feature.common.NextContestCard
import io.duckling.contestpulse.feature.common.PageHeader
import io.duckling.contestpulse.feature.common.SelectableChip
import io.duckling.contestpulse.feature.common.label
import io.duckling.contestpulse.feature.common.localDateTimeLabel

@Composable
fun ContestListRoute(
    onContestClick: (String) -> Unit,
    sharedElementModifier: @Composable (Contest) -> Modifier,
    viewModel: ContestListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ContestListScreen(
        uiState = uiState,
        onContestClick = onContestClick,
        onToggleFavorite = viewModel::toggleFavorite,
        onRefresh = viewModel::refresh,
        onToggleFilterExpanded = viewModel::toggleFilterExpanded,
        onToggleSource = viewModel::toggleSource,
        onSelectDateRange = viewModel::selectDateRange,
        onToggleRatedOnly = viewModel::toggleRatedOnly,
        onToggleFavoriteOnly = viewModel::toggleFavoriteOnly,
        onClearFilters = viewModel::clearFilters,
        sharedElementModifier = sharedElementModifier,
    )
}

@Composable
fun ContestListScreen(
    uiState: ContestListUiState,
    onContestClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    sharedElementModifier: @Composable (Contest) -> Modifier,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    onToggleFilterExpanded: () -> Unit = {},
    onToggleSource: (ContestSource) -> Unit = {},
    onSelectDateRange: (ContestDateRange) -> Unit = {},
    onToggleRatedOnly: () -> Unit = {},
    onToggleFavoriteOnly: () -> Unit = {},
    onClearFilters: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        FadeTransition(
            visible = uiState.isLoading,
            modifier = Modifier.fillMaxSize(),
        ) {
            LoadingContestList()
        }
        FadeTransition(
            visible = !uiState.isLoading,
            modifier = Modifier.fillMaxSize(),
        ) {
            ContestListContent(
                uiState = uiState,
                onContestClick = onContestClick,
                onToggleFavorite = onToggleFavorite,
                onRefresh = onRefresh,
                onToggleFilterExpanded = onToggleFilterExpanded,
                onToggleSource = onToggleSource,
                onSelectDateRange = onSelectDateRange,
                onToggleRatedOnly = onToggleRatedOnly,
                onToggleFavoriteOnly = onToggleFavoriteOnly,
                onClearFilters = onClearFilters,
                sharedElementModifier = sharedElementModifier,
            )
        }
    }
}

@Composable
private fun LoadingContestList() {
    LazyColumn(
        contentPadding = topLevelScreenPadding(),
    ) {
        item(key = "loading-header") {
            PageHeader(
                eyebrow = stringResource(R.string.contest_list_eyebrow),
                title = stringResource(R.string.contest_list_title),
                subtitle = stringResource(R.string.contest_list_subtitle),
            )
            Spacer(modifier = Modifier.height(PulseTheme.spacing.xxl))
        }
        item(key = "loading-cards") {
            LoadingCards()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContestListContent(
    uiState: ContestListUiState,
    onContestClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRefresh: () -> Unit,
    onToggleFilterExpanded: () -> Unit,
    onToggleSource: (ContestSource) -> Unit,
    onSelectDateRange: (ContestDateRange) -> Unit,
    onToggleRatedOnly: () -> Unit,
    onToggleFavoriteOnly: () -> Unit,
    onClearFilters: () -> Unit,
    sharedElementModifier: @Composable (Contest) -> Modifier,
) {
    val pullState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullRefreshIndicator(
                state = pullState,
                isRefreshing = uiState.isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = topLevelScreenPadding(),
        ) {
            item(key = "header") {
                PageHeader(
                    eyebrow = stringResource(R.string.contest_list_eyebrow),
                    title = stringResource(R.string.contest_list_title),
                    subtitle = stringResource(R.string.contest_list_subtitle),
                )
                Spacer(modifier = Modifier.height(PulseTheme.spacing.lg))
                SyncStatusControl(
                    isRefreshing = uiState.isRefreshing,
                    issueType = uiState.syncIssueType,
                    failedSources = uiState.failedSources,
                    hasSuccessfulSource = uiState.hasSuccessfulSource,
                    hasCompletedSync = uiState.hasCompletedSync,
                    lastUpdatedAt = uiState.lastUpdatedAt,
                    onRefresh = onRefresh,
                )
                Spacer(modifier = Modifier.height(PulseTheme.spacing.lg))
                ContestFilterPanel(
                    uiState = uiState,
                    onToggleExpanded = onToggleFilterExpanded,
                    onToggleSource = onToggleSource,
                    onSelectDateRange = onSelectDateRange,
                    onToggleRatedOnly = onToggleRatedOnly,
                    onToggleFavoriteOnly = onToggleFavoriteOnly,
                    onClearFilters = onClearFilters,
                )
                Spacer(modifier = Modifier.height(PulseTheme.spacing.xxl))
            }

            item(key = "next-contest") {
                FadeTransition(visible = uiState.nextContest != null) {
                    uiState.nextContest?.let { nextContest ->
                        NextContestCard(
                            contest = nextContest,
                            now = uiState.now,
                            onClick = { onContestClick(nextContest.id) },
                            modifier = sharedElementModifier(nextContest),
                        )
                        Spacer(modifier = Modifier.height(PulseTheme.spacing.xxxl))
                    }
                }
            }

            uiState.groups.forEach { group ->
                item(key = "group-${group.type.name}") {
                    Text(
                        text = group.type.label(),
                        color = PulseTheme.colors.textPrimary,
                        style = PulseTheme.typography.title3,
                    )
                    Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
                }
                items(
                    items = group.contests,
                    key = Contest::id,
                ) { contest ->
                    ContestCard(
                        contest = contest,
                        now = uiState.now,
                        onClick = { onContestClick(contest.id) },
                        onToggleFavorite = { onToggleFavorite(contest.id) },
                        modifier = sharedElementModifier(contest),
                    )
                    Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
                }
                item(key = "group-space-${group.type.name}") {
                    Spacer(modifier = Modifier.height(PulseTheme.spacing.lg))
                }
            }

            item(key = "empty") {
                FadeTransition(
                    visible = uiState.groups.isEmpty() && uiState.nextContest == null,
                ) {
                    EmptyState(
                        title = stringResource(
                            if (uiState.activeFilterCount > 0) {
                                R.string.filter_empty_title
                            } else {
                                R.string.contest_empty_title
                            },
                        ),
                        body = stringResource(
                            when {
                                uiState.activeFilterCount > 0 -> R.string.filter_empty_body
                                uiState.syncIssueType != null -> R.string.contest_empty_offline_body
                                else -> R.string.contest_empty_body
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ContestFilterPanel(
    uiState: ContestListUiState,
    onToggleExpanded: () -> Unit,
    onToggleSource: (ContestSource) -> Unit,
    onSelectDateRange: (ContestDateRange) -> Unit,
    onToggleRatedOnly: () -> Unit,
    onToggleFavoriteOnly: () -> Unit,
    onClearFilters: () -> Unit,
) {
    val isExpanded = uiState.isFilterExpanded
    val panelState = stringResource(
        if (isExpanded) R.string.filter_state_expanded else R.string.filter_state_collapsed,
    )
    val summary = if (uiState.activeFilterCount == 0) {
        stringResource(R.string.filter_summary_all)
    } else {
        stringResource(
            R.string.filter_summary_active,
            uiState.activeFilterCount,
            uiState.filteredContestCount,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PulseTheme.colors.surfaceMuted,
                shape = RoundedCornerShape(PulseTheme.radius.lg),
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy, // keep panel expansion controlled
                    stiffness = Spring.StiffnessMedium, // reveal controls without feeling delayed
                ),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { stateDescription = panelState }
                .pressEffect(
                    contentDescription = stringResource(
                        if (isExpanded) {
                            R.string.filter_action_collapse
                        } else {
                            R.string.filter_action_expand
                        },
                    ),
                    role = Role.Button,
                    onClick = onToggleExpanded,
                )
                .padding(PulseTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.filter_title),
                    color = PulseTheme.colors.textPrimary,
                    style = PulseTheme.typography.title3,
                )
                Spacer(modifier = Modifier.height(PulseTheme.spacing.xxs))
                Text(
                    text = summary,
                    color = PulseTheme.colors.textSecondary,
                    style = PulseTheme.typography.footnote,
                )
            }
            Text(
                text = stringResource(
                    if (isExpanded) R.string.filter_collapse else R.string.filter_expand,
                ),
                color = PulseTheme.colors.textPrimary,
                style = PulseTheme.typography.footnote,
            )
        }

        FadeTransition(visible = isExpanded) {
            Column(
                modifier = Modifier.padding(
                    start = PulseTheme.spacing.lg,
                    end = PulseTheme.spacing.lg,
                    bottom = PulseTheme.spacing.lg,
                ),
            ) {
                FilterSectionLabel(text = stringResource(R.string.filter_sources_label))
                Spacer(modifier = Modifier.height(PulseTheme.spacing.sm))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.xs),
                ) {
                    items(
                        items = FILTERABLE_SOURCES,
                        key = ContestSource::name,
                    ) { source ->
                        SelectableChip(
                            label = source.label(),
                            selected = source in uiState.filter.sources,
                            onClick = { onToggleSource(source) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(PulseTheme.spacing.lg))
                FilterSectionLabel(text = stringResource(R.string.filter_date_label))
                Spacer(modifier = Modifier.height(PulseTheme.spacing.sm))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.xs),
                ) {
                    items(
                        items = ContestDateRange.entries,
                        key = ContestDateRange::name,
                    ) { range ->
                        SelectableChip(
                            label = range.label(),
                            selected = uiState.filter.dateRange == range,
                            onClick = { onSelectDateRange(range) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(PulseTheme.spacing.lg))
                FilterSectionLabel(text = stringResource(R.string.filter_quick_label))
                Spacer(modifier = Modifier.height(PulseTheme.spacing.sm))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.xs),
                ) {
                    item(key = "rated-only") {
                        SelectableChip(
                            label = stringResource(R.string.filter_rated_only),
                            selected = uiState.filter.ratedOnly,
                            onClick = onToggleRatedOnly,
                        )
                    }
                    item(key = "favorite-only") {
                        SelectableChip(
                            label = stringResource(R.string.filter_favorite_only),
                            selected = uiState.filter.favoriteOnly,
                            onClick = onToggleFavoriteOnly,
                        )
                    }
                }
                FadeTransition(visible = uiState.activeFilterCount > 0) {
                    Column {
                        Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.pressEffect(
                                contentDescription = stringResource(R.string.filter_clear),
                                onClick = onClearFilters,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.filter_clear),
                                color = PulseTheme.colors.textPrimary,
                                style = PulseTheme.typography.footnote,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSectionLabel(text: String) {
    Text(
        text = text,
        color = PulseTheme.colors.textTertiary,
        style = PulseTheme.typography.caption1,
    )
}

@Composable
private fun ContestDateRange.label(): String = stringResource(
    when (this) {
        ContestDateRange.ALL -> R.string.filter_date_all
        ContestDateRange.NEXT_7_DAYS -> R.string.filter_date_week
        ContestDateRange.NEXT_30_DAYS -> R.string.filter_date_month
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PullRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = PulseTheme.spacing
    val density = LocalDensity.current
    val pullRange = with(density) { spacing.giant.toPx() }
    val hiddenOffset = with(density) { spacing.huge.toPx() }
    val translation = state.distanceFraction
        .coerceIn(0f, MAX_INDICATOR_DISTANCE_FRACTION) * pullRange - hiddenOffset
    val status = stringResource(
        when {
            isRefreshing -> R.string.sync_status_refreshing
            state.distanceFraction >= 1f -> R.string.sync_pull_release
            else -> R.string.sync_pull_hint
        },
    )

    FadeTransition(
        visible = isRefreshing || state.distanceFraction > MIN_INDICATOR_DISTANCE_FRACTION,
        modifier = modifier.graphicsLayer { translationY = translation },
    ) {
        Text(
            text = status,
            color = PulseTheme.colors.textPrimary,
            style = PulseTheme.typography.footnote,
            modifier = Modifier
                .background(
                    color = PulseTheme.colors.surfaceMuted,
                    shape = RoundedCornerShape(PulseTheme.radius.full),
                )
                .semantics { liveRegion = LiveRegionMode.Polite }
                .padding(
                    horizontal = PulseTheme.spacing.md,
                    vertical = PulseTheme.spacing.sm,
                ),
        )
    }
}

@Composable
private fun SyncStatusControl(
    isRefreshing: Boolean,
    issueType: SyncErrorType?,
    failedSources: List<FailedSyncSource>,
    hasSuccessfulSource: Boolean,
    hasCompletedSync: Boolean,
    lastUpdatedAt: java.time.Instant?,
    onRefresh: () -> Unit,
) {
    val failedSourceText = failedSourceLabel(failedSources)
    val statusText = when {
        isRefreshing -> stringResource(R.string.sync_status_refreshing)
        issueType != null && failedSourceText.isBlank() -> stringResource(R.string.sync_status_failed)
        issueType != null && hasSuccessfulSource -> stringResource(
            R.string.sync_status_partial_failed,
            failedSourceText,
        )
        issueType != null -> stringResource(R.string.sync_status_sources_failed, failedSourceText)
        lastUpdatedAt != null -> stringResource(
            R.string.sync_status_updated,
            lastUpdatedAt.localDateTimeLabel(R.string.contest_time_pattern),
        )
        hasCompletedSync -> stringResource(R.string.sync_status_no_upcoming)
        else -> stringResource(R.string.sync_status_ready)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PulseTheme.colors.surfaceMuted,
                shape = RoundedCornerShape(PulseTheme.radius.lg),
            )
            .pressEffect(
                contentDescription = stringResource(R.string.sync_action_refresh),
                role = Role.Button,
                enabled = !isRefreshing,
                onClick = onRefresh,
            )
            .padding(
                horizontal = PulseTheme.spacing.lg,
                vertical = PulseTheme.spacing.md,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = statusText,
            color = if (issueType == null) {
                PulseTheme.colors.textSecondary
            } else {
                PulseTheme.colors.textPrimary
            },
            style = PulseTheme.typography.footnote,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(
                if (isRefreshing) R.string.sync_action_wait else R.string.sync_action_refresh,
            ),
            color = PulseTheme.colors.textPrimary,
            style = PulseTheme.typography.footnote,
        )
    }
}

@Composable
private fun failedSourceLabel(failedSources: List<FailedSyncSource>): String {
    val labels = ArrayList<String>(failedSources.size)
    for (failed in failedSources) {
        labels += failed.displayName ?: failed.source.label()
    }
    return labels.joinToString(separator = "、")
}

private val FILTERABLE_SOURCES = listOf(
    ContestSource.CODEFORCES,
    ContestSource.ATCODER,
    ContestSource.LUOGU,
    ContestSource.NOWCODER,
    ContestSource.OTHER,
)
private const val MIN_INDICATOR_DISTANCE_FRACTION = 0.01f
private const val MAX_INDICATOR_DISTANCE_FRACTION = 1.5f
