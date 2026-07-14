package io.duckling.contestpulse.feature.favorites

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.duckling.contestpulse.R
import io.duckling.contestpulse.core.designsystem.component.FadeTransition
import io.duckling.contestpulse.core.designsystem.component.topLevelScreenPadding
import io.duckling.contestpulse.core.designsystem.theme.PulseTheme
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.feature.common.ContestCard
import io.duckling.contestpulse.feature.common.EmptyState
import io.duckling.contestpulse.feature.common.LoadingCards
import io.duckling.contestpulse.feature.common.PageHeader
import io.duckling.contestpulse.feature.common.SelectableChip

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
                        title = stringResource(R.string.favorites_title),
                        subtitle = stringResource(R.string.favorites_subtitle),
                    )
                    Spacer(modifier = Modifier.height(PulseTheme.spacing.xl))
                    FavoriteSegments(
                        selected = uiState.selectedSegment,
                        onSelect = onSelectSegment,
                    )
                    Spacer(modifier = Modifier.height(PulseTheme.spacing.xxl))
                }

                items(
                    items = uiState.contests,
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
