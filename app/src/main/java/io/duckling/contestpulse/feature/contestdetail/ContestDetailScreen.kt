package io.duckling.contestpulse.feature.contestdetail

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.duckling.contestpulse.R
import io.duckling.contestpulse.core.designsystem.component.FadeTransition
import io.duckling.contestpulse.core.calendar.addContestToSystemCalendar
import io.duckling.contestpulse.core.designsystem.component.appCard
import io.duckling.contestpulse.core.designsystem.component.pressEffect
import io.duckling.contestpulse.core.designsystem.theme.PulseTheme
import io.duckling.contestpulse.core.navigation.openWebUrl
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.feature.common.EmptyState
import io.duckling.contestpulse.feature.common.PlatformBadge
import io.duckling.contestpulse.feature.common.SelectableChip
import io.duckling.contestpulse.feature.common.countdownLabel
import io.duckling.contestpulse.feature.common.durationLabel
import io.duckling.contestpulse.feature.common.localDateTimeLabel
import java.time.Duration

@Composable
fun ContestDetailRoute(
    onBack: () -> Unit,
    sharedElementModifier: @Composable (Contest) -> Modifier,
    viewModel: ContestDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingReminderOffset by remember { mutableStateOf<Duration?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val offset = pendingReminderOffset
        pendingReminderOffset = null
        if (granted && offset != null) {
            viewModel.toggleReminder(offset)
        } else if (!granted) {
            viewModel.notificationPermissionDenied()
        }
    }
    val toggleReminder: (Duration) -> Unit = { offset ->
        val isRemoving = offset in uiState.contest?.reminderOffsets.orEmpty()
        val needsPermission = !isRemoving &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingReminderOffset = offset
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.toggleReminder(offset)
        }
    }
    ContestDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onToggleFavorite = viewModel::toggleFavorite,
        onOpenOfficialPage = { url -> context.openWebUrl(url) },
        onAddToCalendar = { contest -> context.addContestToSystemCalendar(contest) },
        onToggleReminder = toggleReminder,
        sharedElementModifier = sharedElementModifier,
    )
}

@Composable
fun ContestDetailScreen(
    uiState: ContestDetailUiState,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenOfficialPage: (String) -> Unit,
    onAddToCalendar: (Contest) -> Unit,
    onToggleReminder: (Duration) -> Unit,
    sharedElementModifier: @Composable (Contest) -> Modifier,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PulseTheme.colors.background)
            .statusBarsPadding(),
    ) {
        FadeTransition(
            visible = uiState.isLoading,
            modifier = Modifier.fillMaxSize(),
        ) {
            DetailLoading(onBack = onBack)
        }
        FadeTransition(
            visible = !uiState.isLoading && uiState.contest == null,
            modifier = Modifier.fillMaxSize(),
        ) {
            DetailNotFound(onBack = onBack)
        }
        FadeTransition(
            visible = !uiState.isLoading && uiState.contest != null,
            modifier = Modifier.fillMaxSize(),
        ) {
            uiState.contest?.let { contest ->
                DetailContent(
                    contest = contest,
                    now = uiState.now,
                    onBack = onBack,
                    onToggleFavorite = onToggleFavorite,
                    onOpenOfficialPage = onOpenOfficialPage,
                    onAddToCalendar = onAddToCalendar,
                    onToggleReminder = onToggleReminder,
                    canScheduleExactReminders = uiState.canScheduleExactReminders,
                    reminderMessage = uiState.reminderMessage,
                    modifier = sharedElementModifier(contest),
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    contest: Contest,
    now: java.time.Instant,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenOfficialPage: (String) -> Unit,
    onAddToCalendar: (Contest) -> Unit,
    onToggleReminder: (Duration) -> Unit,
    canScheduleExactReminders: Boolean,
    reminderMessage: ReminderUiMessage?,
    modifier: Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = PulseTheme.spacing.xl,
                vertical = PulseTheme.spacing.md,
            ),
    ) {
        BackButton(onBack = onBack)
        Spacer(modifier = Modifier.height(PulseTheme.spacing.xl))
        Column(
            modifier = modifier
                .fillMaxWidth()
                .appCard()
                .padding(PulseTheme.spacing.xl),
        ) {
            PlatformBadge(contest = contest)
            Spacer(modifier = Modifier.height(PulseTheme.spacing.xl))
            Text(
                text = stringResource(R.string.detail_eyebrow),
                color = PulseTheme.colors.textTertiary,
                style = PulseTheme.typography.caption1,
            )
            Spacer(modifier = Modifier.height(PulseTheme.spacing.sm))
            Text(
                text = contest.title,
                color = PulseTheme.colors.textPrimary,
                style = PulseTheme.typography.title1,
            )
            Spacer(modifier = Modifier.height(PulseTheme.spacing.xl))
            Text(
                text = contest.countdownLabel(now),
                color = PulseTheme.colors.textPrimary,
                style = PulseTheme.typography.largeTitle,
            )
        }
        Spacer(modifier = Modifier.height(PulseTheme.spacing.xxl))
        DetailFacts(contest = contest)
        Spacer(modifier = Modifier.height(PulseTheme.spacing.xxl))
        DetailReminderSection(
            contest = contest,
            canScheduleExactReminders = canScheduleExactReminders,
            message = reminderMessage,
            onToggleReminder = onToggleReminder,
        )
        Spacer(modifier = Modifier.height(PulseTheme.spacing.xxl))
        PrimaryOfficialAction(
            onClick = { onOpenOfficialPage(contest.contestUrl) },
        )
        Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
        SecondaryAction(
            label = stringResource(R.string.detail_add_calendar),
            onClick = { onAddToCalendar(contest) },
        )
        Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
        FavoriteAction(
            isFavorite = contest.isFavorite,
            onClick = onToggleFavorite,
        )
        Spacer(modifier = Modifier.height(PulseTheme.spacing.xxl))
    }
}

@Composable
private fun SecondaryAction(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PulseTheme.colors.surfaceMuted,
                shape = RoundedCornerShape(PulseTheme.radius.lg),
            )
            .pressEffect(
                contentDescription = label,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(PulseTheme.spacing.md),
    ) {
        Text(
            text = label,
            color = PulseTheme.colors.textPrimary,
            style = PulseTheme.typography.headline,
        )
    }
}

@Composable
private fun DetailReminderSection(
    contest: Contest,
    canScheduleExactReminders: Boolean,
    message: ReminderUiMessage?,
    onToggleReminder: (Duration) -> Unit,
) {
    var customMinutes by rememberSaveable(contest.id) { mutableStateOf("") }
    val parsedCustomMinutes = customMinutes.toLongOrNull()?.takeIf { it > 0 }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appCard()
            .padding(PulseTheme.spacing.xl),
    ) {
        Text(
            text = stringResource(R.string.reminder_title),
            color = PulseTheme.colors.textPrimary,
            style = PulseTheme.typography.title3,
        )
        Spacer(modifier = Modifier.height(PulseTheme.spacing.xs))
        Text(
            text = stringResource(
                if (canScheduleExactReminders) {
                    R.string.reminder_exact_body
                } else {
                    R.string.reminder_inexact_body
                },
            ),
            color = PulseTheme.colors.textSecondary,
            style = PulseTheme.typography.callout,
        )
        Spacer(modifier = Modifier.height(PulseTheme.spacing.lg))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.sm),
        ) {
            REMINDER_PRESETS.forEach { preset ->
                SelectableChip(
                    label = stringResource(preset.labelRes),
                    selected = preset.offset in contest.reminderOffsets,
                    onClick = { onToggleReminder(preset.offset) },
                )
            }
            val presetOffsets = REMINDER_PRESETS.map(ReminderPreset::offset).toSet()
            contest.reminderOffsets
                .filterNot { offset -> offset in presetOffsets }
                .sorted()
                .forEach { offset ->
                    SelectableChip(
                        label = stringResource(
                            R.string.reminder_offset_custom_value,
                            offset.toMinutes(),
                        ),
                        selected = true,
                        onClick = { onToggleReminder(offset) },
                    )
                }
        }
        Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = customMinutes,
                onValueChange = { value ->
                    customMinutes = value.filter(Char::isDigit).take(MAX_CUSTOM_DIGITS)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = PulseTheme.typography.body.copy(
                    color = PulseTheme.colors.textPrimary,
                ),
                cursorBrush = SolidColor(PulseTheme.colors.textPrimary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = PulseTheme.colors.background,
                                shape = RoundedCornerShape(PulseTheme.radius.md),
                            )
                            .padding(PulseTheme.spacing.md),
                    ) {
                        if (customMinutes.isEmpty()) {
                            Text(
                                text = stringResource(R.string.reminder_custom_hint),
                                color = PulseTheme.colors.textTertiary,
                                style = PulseTheme.typography.body,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(
                        color = if (parsedCustomMinutes != null) {
                            PulseTheme.colors.accent
                        } else {
                            PulseTheme.colors.separator
                        },
                        shape = RoundedCornerShape(PulseTheme.radius.md),
                    )
                    .pressEffect(
                        contentDescription = stringResource(R.string.reminder_custom_add),
                        enabled = parsedCustomMinutes != null,
                        onClick = {
                            parsedCustomMinutes?.let { minutes ->
                                onToggleReminder(Duration.ofMinutes(minutes))
                                customMinutes = ""
                            }
                        },
                    )
                    .padding(horizontal = PulseTheme.spacing.md),
            ) {
                Text(
                    text = stringResource(R.string.reminder_custom_add),
                    color = if (parsedCustomMinutes != null) {
                        PulseTheme.colors.onAccent
                    } else {
                        PulseTheme.colors.textTertiary
                    },
                    style = PulseTheme.typography.footnote,
                )
            }
        }
        message?.let { reminderMessage ->
            Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
            Text(
                text = reminderMessage.label(),
                color = PulseTheme.colors.textPrimary,
                style = PulseTheme.typography.footnote,
            )
        }
    }
}

@Composable
private fun ReminderUiMessage.label(): String = stringResource(
    when (this) {
        ReminderUiMessage.SCHEDULED_EXACT -> R.string.reminder_result_exact
        ReminderUiMessage.SCHEDULED_INEXACT -> R.string.reminder_result_inexact
        ReminderUiMessage.REMOVED -> R.string.reminder_result_removed
        ReminderUiMessage.TOO_LATE -> R.string.reminder_result_too_late
        ReminderUiMessage.NOTIFICATION_PERMISSION_DENIED ->
            R.string.reminder_result_permission_denied
        ReminderUiMessage.FAILED -> R.string.reminder_result_failed
    },
)

@Composable
private fun DetailFacts(contest: Contest) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appCard()
            .padding(PulseTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.lg),
    ) {
        DetailFact(
            label = stringResource(R.string.detail_start_time),
            value = contest.startTime.localDateTimeLabel(),
        )
        DetailFact(
            label = stringResource(R.string.detail_end_time),
            value = contest.endTime?.localDateTimeLabel()
                ?: stringResource(R.string.detail_unknown),
        )
        DetailFact(
            label = stringResource(R.string.detail_duration),
            value = contest.duration.durationLabel(),
        )
        DetailFact(
            label = stringResource(R.string.detail_category),
            value = contest.category ?: stringResource(R.string.detail_unknown),
        )
        DetailFact(
            label = stringResource(R.string.detail_rated_range),
            value = contest.ratedRange ?: stringResource(R.string.detail_unknown),
        )
        DetailFact(
            label = stringResource(R.string.detail_last_updated),
            value = contest.lastUpdatedAt.localDateTimeLabel(R.string.contest_time_pattern),
        )
    }
}

@Composable
private fun DetailFact(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            color = PulseTheme.colors.textTertiary,
            style = PulseTheme.typography.subheadline,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = PulseTheme.colors.textPrimary,
            style = PulseTheme.typography.subheadline,
            modifier = Modifier.weight(1.4f),
        )
    }
}

@Composable
private fun PrimaryOfficialAction(onClick: () -> Unit) {
    val label = stringResource(R.string.detail_open_official)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PulseTheme.colors.accent,
                shape = RoundedCornerShape(PulseTheme.radius.lg),
            )
            .pressEffect(
                contentDescription = label,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(PulseTheme.spacing.md),
    ) {
        Text(
            text = label,
            color = PulseTheme.colors.onAccent,
            style = PulseTheme.typography.headline,
        )
    }
}

@Composable
private fun FavoriteAction(
    isFavorite: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(
        if (isFavorite) R.string.favorite_remove else R.string.favorite_add,
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PulseTheme.colors.surfaceMuted,
                shape = RoundedCornerShape(PulseTheme.radius.lg),
            )
            .pressEffect(
                contentDescription = label,
                role = Role.Button,
                onClick = onClick,
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy, // keep the label swap stable
                    stiffness = Spring.StiffnessMedium, // balanced resize response
                ),
            )
            .padding(PulseTheme.spacing.md),
    ) {
        Text(
            text = label,
            color = PulseTheme.colors.textPrimary,
            style = PulseTheme.typography.headline,
        )
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.pressEffect(
            contentDescription = stringResource(R.string.detail_back),
            onClick = onBack,
        ),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_back),
            contentDescription = null,
            tint = PulseTheme.colors.textPrimary,
            modifier = Modifier.height(PulseTheme.spacing.xl),
        )
    }
}

@Composable
private fun DetailLoading(onBack: () -> Unit) {
    Column(
        modifier = Modifier.padding(
            horizontal = PulseTheme.spacing.xl,
            vertical = PulseTheme.spacing.md,
        ),
    ) {
        BackButton(onBack = onBack)
        Spacer(modifier = Modifier.height(PulseTheme.spacing.xxl))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PulseTheme.spacing.giant * LOADING_HEIGHT_MULTIPLIER)
                .background(
                    PulseTheme.colors.surfaceMuted,
                    RoundedCornerShape(PulseTheme.radius.lg),
                ),
        )
    }
}

@Composable
private fun DetailNotFound(onBack: () -> Unit) {
    Column(
        modifier = Modifier.padding(
            horizontal = PulseTheme.spacing.xl,
            vertical = PulseTheme.spacing.md,
        ),
    ) {
        BackButton(onBack = onBack)
        EmptyState(
            title = stringResource(R.string.detail_not_found_title),
            body = stringResource(R.string.detail_not_found_body),
        )
    }
}

private const val LOADING_HEIGHT_MULTIPLIER = 4

private data class ReminderPreset(
    val offset: Duration,
    val labelRes: Int,
)

private val REMINDER_PRESETS = listOf(
    ReminderPreset(Duration.ofDays(1), R.string.reminder_offset_day),
    ReminderPreset(Duration.ofHours(3), R.string.reminder_offset_three_hours),
    ReminderPreset(Duration.ofHours(1), R.string.reminder_offset_hour),
    ReminderPreset(Duration.ofMinutes(15), R.string.reminder_offset_fifteen_minutes),
    ReminderPreset(Duration.ZERO, R.string.reminder_offset_start),
)

private const val MAX_CUSTOM_DIGITS = 5
