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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import io.duckling.contestpulse.domain.model.ReminderDefinition
import io.duckling.contestpulse.domain.model.ReminderMode
import io.duckling.contestpulse.domain.model.ReminderRule
import io.duckling.contestpulse.feature.reminder.AddReminderDialog
import io.duckling.contestpulse.feature.reminder.ReminderList
import io.duckling.contestpulse.feature.reminder.ReminderListItemModel
import io.duckling.contestpulse.domain.logic.ReminderTriggerResult
import io.duckling.contestpulse.domain.logic.calculateReminderTrigger
import io.duckling.contestpulse.feature.common.EmptyState
import io.duckling.contestpulse.feature.common.PlatformBadge
import io.duckling.contestpulse.feature.common.countdownLabel
import io.duckling.contestpulse.feature.common.durationLabel
import io.duckling.contestpulse.feature.common.localDateTimeLabel

@Composable
fun ContestDetailRoute(
    onBack: () -> Unit,
    sharedElementModifier: @Composable (Contest) -> Modifier,
    viewModel: ContestDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            viewModel.notificationPermissionDenied()
        }
    }
    val saveReminder: (String?, ReminderRule) -> Unit = { reminderId, rule ->
        val needsPermission =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            viewModel.saveReminder(reminderId, rule)
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.saveReminder(reminderId, rule)
        }
    }
    ContestDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onToggleFavorite = viewModel::toggleFavorite,
        onOpenOfficialPage = { url -> context.openWebUrl(url) },
        onAddToCalendar = { contest -> context.addContestToSystemCalendar(contest) },
        onSaveReminder = saveReminder,
        onDeleteReminder = viewModel::deleteReminder,
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
    onSaveReminder: (String?, ReminderRule) -> Unit,
    onDeleteReminder: (String) -> Unit,
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
                    onSaveReminder = onSaveReminder,
                    onDeleteReminder = onDeleteReminder,
                    defaultReminders = uiState.defaultReminders,
                    zoneId = uiState.zoneId,
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
    onSaveReminder: (String?, ReminderRule) -> Unit,
    onDeleteReminder: (String) -> Unit,
    defaultReminders: List<ReminderDefinition>,
    zoneId: java.time.ZoneId,
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
                text = contest.countdownLabel(now, zoneId),
                color = PulseTheme.colors.textPrimary,
                style = PulseTheme.typography.largeTitle,
            )
        }
        Spacer(modifier = Modifier.height(PulseTheme.spacing.xxl))
        DetailFacts(contest = contest)
        Spacer(modifier = Modifier.height(PulseTheme.spacing.xxl))
        DetailReminderSection(
            contest = contest,
            now = now,
            canScheduleExactReminders = canScheduleExactReminders,
            message = reminderMessage,
            defaultReminders = defaultReminders,
            zoneId = zoneId,
            onSaveReminder = onSaveReminder,
            onDeleteReminder = onDeleteReminder,
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
    now: java.time.Instant,
    canScheduleExactReminders: Boolean,
    message: ReminderUiMessage?,
    defaultReminders: List<ReminderDefinition>,
    zoneId: java.time.ZoneId,
    onSaveReminder: (String?, ReminderRule) -> Unit,
    onDeleteReminder: (String) -> Unit,
) {
    var showEditor by rememberSaveable(contest.id) { mutableStateOf(false) }
    var editingId by rememberSaveable(contest.id) { mutableStateOf<String?>(null) }
    val usesStoredRules = contest.isFavorite ||
        contest.reminderMode == ReminderMode.CUSTOM ||
        contest.reminders.isNotEmpty()
    val applicableDefaults = defaultReminders.mapNotNull { definition ->
        val trigger = calculateReminderTrigger(
            contestStart = contest.startTime,
            rule = definition.rule,
            now = now,
            zoneId = zoneId,
        ) as? ReminderTriggerResult.Valid
        trigger?.let { definition to it.triggerAt }
    }.sortedWith(
        compareBy<Pair<ReminderDefinition, java.time.Instant>>(
            { it.second },
            { it.first.createdAt },
            { it.first.id },
        ),
    )
    val skippedDefaultCount = defaultReminders.size - applicableDefaults.size
    val reminderItems = if (usesStoredRules) {
        contest.reminders
            .sortedWith(
                compareBy(
                    { reminder -> reminder.triggerAt ?: java.time.Instant.MAX },
                    { reminder -> reminder.definition.createdAt },
                    { reminder -> reminder.id },
                ),
            )
            .map { reminder ->
                ReminderListItemModel(
                    id = reminder.id,
                    rule = reminder.rule,
                    scheduleStatus = reminder.scheduleStatus,
                    deliveryStatus = reminder.deliveryStatus,
                )
            }
    } else {
        applicableDefaults.map { (definition, _) ->
            ReminderListItemModel(
                id = definition.id,
                rule = definition.rule,
                isPreview = true,
            )
        }
    }
    val editing = reminderItems.firstOrNull { it.id == editingId }
    val contestEnded = (contest.endTime ?: contest.startTime) <= now
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
        if (reminderItems.isEmpty()) {
            Text(
                text = "尚未设置提醒",
                color = PulseTheme.colors.textTertiary,
                style = PulseTheme.typography.callout,
            )
        } else {
            ReminderList(
                reminders = reminderItems,
                onEdit = { item ->
                    editingId = item.id
                    showEditor = true
                },
                onDelete = { item -> onDeleteReminder(item.id) },
            )
        }
        if (!usesStoredRules && skippedDefaultCount > 0) {
            Spacer(modifier = Modifier.height(PulseTheme.spacing.xs))
            Text(
                text = "${skippedDefaultCount} 条默认提醒不适用于本场比赛，已跳过",
                color = PulseTheme.colors.textTertiary,
                style = PulseTheme.typography.caption1,
            )
        }
        Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
        if (contestEnded) {
            Text(
                text = "比赛已结束，无法新增提醒",
                color = PulseTheme.colors.textTertiary,
                style = PulseTheme.typography.footnote,
            )
        } else {
            val addLabel = "新增提醒"
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = PulseTheme.colors.accent,
                        shape = RoundedCornerShape(PulseTheme.radius.md),
                    )
                    .pressEffect(
                        contentDescription = addLabel,
                        role = Role.Button,
                        onClick = {
                            editingId = null
                            showEditor = true
                        },
                    )
                    .padding(PulseTheme.spacing.md),
            ) {
                Text(
                    text = addLabel,
                    color = PulseTheme.colors.onAccent,
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
    AddReminderDialog(
        visible = showEditor,
        editing = editing,
        existingRules = reminderItems.map(ReminderListItemModel::rule),
        contestStart = contest.startTime,
        now = now,
        zoneId = zoneId,
        onDismiss = { showEditor = false },
        onSave = { rule ->
            onSaveReminder(editing?.id, rule)
            showEditor = false
        },
    )
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
        ReminderUiMessage.DUPLICATE -> R.string.reminder_result_duplicate
        ReminderUiMessage.INVALID -> R.string.reminder_result_invalid
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
