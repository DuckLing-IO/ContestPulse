package io.duckling.contestpulse.feature.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.duckling.contestpulse.R
import io.duckling.contestpulse.core.designsystem.component.FadeTransition
import io.duckling.contestpulse.core.designsystem.component.appCard
import io.duckling.contestpulse.core.designsystem.component.pressEffect
import io.duckling.contestpulse.core.designsystem.theme.PulseTheme
import io.duckling.contestpulse.domain.model.Contest
import java.time.Instant

@Composable
fun PageHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = eyebrow,
            color = PulseTheme.colors.textTertiary,
            style = PulseTheme.typography.caption1,
        )
        Spacer(modifier = Modifier.height(PulseTheme.spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = PulseTheme.colors.textPrimary,
                style = PulseTheme.typography.largeTitle,
                modifier = Modifier.weight(1f),
            )
            trailingContent?.invoke()
        }
        Spacer(modifier = Modifier.height(PulseTheme.spacing.sm))
        Text(
            text = subtitle,
            color = PulseTheme.colors.textSecondary,
            style = PulseTheme.typography.body,
        )
    }
}

@Composable
fun PlatformBadge(
    contest: Contest,
    modifier: Modifier = Modifier,
) {
    Text(
        text = contest.platformLabel(),
        color = PulseTheme.colors.textSecondary,
        style = PulseTheme.typography.footnote.copy(fontWeight = FontWeight.SemiBold),
        modifier = modifier
            .background(
                color = PulseTheme.colors.surface,
                shape = RoundedCornerShape(PulseTheme.radius.full),
            )
            .padding(
                horizontal = PulseTheme.spacing.sm,
                vertical = PulseTheme.spacing.xs,
            ),
    )
}

@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        if (isFavorite) R.string.favorite_remove else R.string.favorite_add,
    )
    val state = stringResource(
        if (isFavorite) R.string.favorite_state_saved else R.string.favorite_state_not_saved,
    )
    val tint by animateColorAsState(
        targetValue = if (isFavorite) {
            PulseTheme.colors.textPrimary
        } else {
            PulseTheme.colors.textTertiary
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, // confirm the saved-state change
            stiffness = Spring.StiffnessHigh, // keep the toggle response immediate
        ),
        label = "favoriteTint",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .semantics { stateDescription = state }
            .pressEffect(
                contentDescription = description,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_favorites),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(PulseTheme.spacing.xl),
        )
    }
}

@Composable
fun ContestCard(
    contest: Contest,
    now: Instant,
    zoneId: java.time.ZoneId,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .appCard()
            .pressEffect(
                contentDescription = stringResource(
                    R.string.contest_open_detail,
                    contest.title,
                ),
                onClick = onClick,
            )
            .padding(PulseTheme.spacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlatformBadge(contest = contest)
            FavoriteButton(
                isFavorite = contest.isFavorite,
                onClick = onToggleFavorite,
            )
        }
        Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
        Text(
            text = contest.title,
            color = PulseTheme.colors.textPrimary,
            style = PulseTheme.typography.headline,
        )
        Spacer(modifier = Modifier.height(PulseTheme.spacing.sm))
        Text(
            text = contest.startTime.localDateTimeLabel(),
            color = PulseTheme.colors.textSecondary,
            style = PulseTheme.typography.subheadline,
        )
        Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = contest.duration.durationLabel(),
                    color = PulseTheme.colors.textTertiary,
                    style = PulseTheme.typography.footnote,
                )
                FadeTransition(visible = contest.reminders.isNotEmpty()) {
                    Column {
                        Spacer(modifier = Modifier.height(PulseTheme.spacing.xxs))
                        Text(
                            text = stringResource(
                                R.string.contest_reminder_count,
                                contest.reminders.size,
                            ),
                            color = PulseTheme.colors.textSecondary,
                            style = PulseTheme.typography.footnote,
                        )
                    }
                }
            }
            Text(
                text = contest.countdownLabel(now, zoneId),
                color = PulseTheme.colors.textPrimary,
                style = PulseTheme.typography.headline,
            )
        }
    }
}

@Composable
fun NextContestCard(
    contest: Contest,
    now: Instant,
    zoneId: java.time.ZoneId,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .appCard()
            .pressEffect(
                contentDescription = stringResource(
                    R.string.next_contest_open,
                    contest.title,
                ),
                onClick = onClick,
            )
            .padding(PulseTheme.spacing.xl),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.next_contest_label),
                color = PulseTheme.colors.textTertiary,
                style = PulseTheme.typography.caption1,
            )
            PlatformBadge(contest = contest)
        }
        Spacer(modifier = Modifier.height(PulseTheme.spacing.xl))
        Text(
            text = contest.title,
            color = PulseTheme.colors.textPrimary,
            style = PulseTheme.typography.title2,
        )
        Spacer(modifier = Modifier.height(PulseTheme.spacing.xl))
        Text(
            text = contest.countdownLabel(now, zoneId),
            color = PulseTheme.colors.textPrimary,
            style = PulseTheme.typography.title1,
        )
        Spacer(modifier = Modifier.height(PulseTheme.spacing.xs))
        Text(
            text = contest.startTime.localDateTimeLabel(),
            color = PulseTheme.colors.textSecondary,
            style = PulseTheme.typography.subheadline,
        )
    }
}

@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        targetValue = if (selected) {
            PulseTheme.colors.separator
        } else {
            PulseTheme.colors.surfaceMuted
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, // subtle selection confirmation
            stiffness = Spring.StiffnessHigh, // fast enough for repeated filtering
        ),
        label = "chipBackground",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                color = background,
                shape = RoundedCornerShape(PulseTheme.radius.full),
            )
            .pressEffect(
                contentDescription = label,
                role = Role.Checkbox,
                onClick = onClick,
            )
            .padding(horizontal = PulseTheme.spacing.md),
    ) {
        Text(
            text = label,
            color = if (selected) {
                PulseTheme.colors.textPrimary
            } else {
                PulseTheme.colors.textSecondary
            },
            style = PulseTheme.typography.footnote.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = PulseTheme.spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = PulseTheme.colors.textPrimary,
            style = PulseTheme.typography.title3,
        )
        Spacer(modifier = Modifier.height(PulseTheme.spacing.sm))
        Text(
            text = body,
            color = PulseTheme.colors.textSecondary,
            style = PulseTheme.typography.callout,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun LoadingCards(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.md),
    ) {
        repeat(LOADING_CARD_COUNT) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = PulseTheme.colors.surfaceMuted,
                        shape = RoundedCornerShape(PulseTheme.radius.lg),
                    )
                    .padding(PulseTheme.spacing.xl),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(FIRST_SKELETON_WIDTH)
                        .height(PulseTheme.spacing.md)
                        .background(
                            PulseTheme.colors.separator,
                            RoundedCornerShape(PulseTheme.radius.sm),
                        ),
                )
                Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(SECOND_SKELETON_WIDTH)
                        .height(PulseTheme.spacing.xl)
                        .background(
                            PulseTheme.colors.separator,
                            RoundedCornerShape(PulseTheme.radius.sm),
                        ),
                )
            }
        }
    }
}

private const val LOADING_CARD_COUNT = 3
private const val FIRST_SKELETON_WIDTH = 0.28f
private const val SECOND_SKELETON_WIDTH = 0.72f
