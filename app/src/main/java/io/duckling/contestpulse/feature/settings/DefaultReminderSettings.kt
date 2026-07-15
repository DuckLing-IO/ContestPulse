package io.duckling.contestpulse.feature.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import io.duckling.contestpulse.R
import io.duckling.contestpulse.core.designsystem.component.FadeTransition
import io.duckling.contestpulse.core.designsystem.component.appCard
import io.duckling.contestpulse.core.designsystem.component.pressEffect
import io.duckling.contestpulse.core.designsystem.theme.PulseTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

@Composable
internal fun DefaultReminderSettingsCard(
    offsetsMinutes: Set<Int>,
    notificationsEnabled: Boolean,
    exactRemindersAvailable: Boolean,
    onOffsetsChange: (Set<Int>) -> Unit,
    onOpenSystemSettings: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appCard()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy, // expands the reminder list without overshoot
                    stiffness = Spring.StiffnessMedium, // keeps disclosure responsive
                ),
            )
            .padding(PulseTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.md),
    ) {
        ReminderCapabilityRow(
            label = stringResource(R.string.settings_notification_label),
            status = stringResource(
                if (notificationsEnabled) R.string.settings_status_available
                else R.string.settings_status_unavailable,
            ),
        )
        ReminderCapabilityRow(
            label = stringResource(R.string.settings_exact_alarm_label),
            status = stringResource(
                if (exactRemindersAvailable) R.string.settings_status_exact
                else R.string.settings_status_inexact,
            ),
        )
        val expandLabel = stringResource(R.string.settings_default_reminder_label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressEffect(
                    contentDescription = stringResource(
                        if (expanded) R.string.settings_reminder_collapse
                        else R.string.settings_reminder_expand,
                    ),
                    onClick = { expanded = !expanded },
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.xxs),
            ) {
                Text(
                    text = expandLabel,
                    color = PulseTheme.colors.textPrimary,
                    style = PulseTheme.typography.headline,
                )
                Text(
                    text = if (offsetsMinutes.isEmpty()) {
                        stringResource(R.string.settings_reminder_none)
                    } else {
                        stringResource(R.string.settings_reminder_count, offsetsMinutes.size)
                    },
                    color = PulseTheme.colors.textTertiary,
                    style = PulseTheme.typography.footnote,
                )
            }
            Text(
                text = if (expanded) "⌃" else "⌄",
                color = PulseTheme.colors.textSecondary,
                style = PulseTheme.typography.title3,
            )
        }
        FadeTransition(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.sm)) {
                Text(
                    text = stringResource(R.string.settings_default_reminder_body),
                    color = PulseTheme.colors.textTertiary,
                    style = PulseTheme.typography.footnote,
                )
                if (offsetsMinutes.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_reminder_empty),
                        color = PulseTheme.colors.textSecondary,
                        style = PulseTheme.typography.callout,
                    )
                } else {
                    offsetsMinutes.sortedDescending().forEach { minutes ->
                        ReminderOffsetRow(
                            minutes = minutes,
                            onRemove = { onOffsetsChange(offsetsMinutes - minutes) },
                        )
                    }
                }
                ReminderPrimaryAction(
                    label = stringResource(R.string.settings_reminder_add),
                    onClick = { showPicker = true },
                )
            }
        }
        ReminderSystemAction(onClick = onOpenSystemSettings)
    }
    if (showPicker) {
        ReminderOffsetPickerSheet(
            existingOffsets = offsetsMinutes,
            onDismiss = { showPicker = false },
            onAdd = { minutes ->
                onOffsetsChange(offsetsMinutes + minutes)
                showPicker = false
            },
        )
    }
}

@Composable
private fun ReminderCapabilityRow(label: String, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = PulseTheme.colors.textPrimary, style = PulseTheme.typography.callout)
        Text(text = status, color = PulseTheme.colors.textSecondary, style = PulseTheme.typography.footnote)
    }
}

@Composable
private fun ReminderOffsetRow(minutes: Int, onRemove: () -> Unit) {
    val removeLabel = stringResource(R.string.settings_reminder_remove_description, reminderOffsetLabel(minutes))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PulseTheme.colors.surface,
                shape = RoundedCornerShape(PulseTheme.radius.md),
            )
            .padding(start = PulseTheme.spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.xxs)) {
            Text(
                text = reminderOffsetLabel(minutes),
                color = PulseTheme.colors.textPrimary,
                style = PulseTheme.typography.callout,
            )
            Text(
                text = stringResource(R.string.settings_reminder_before_start),
                color = PulseTheme.colors.textTertiary,
                style = PulseTheme.typography.caption1,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(PulseTheme.spacing.giant)
                .pressEffect(contentDescription = removeLabel, onClick = onRemove),
        ) {
            Text(
                text = stringResource(R.string.settings_reminder_remove),
                color = PulseTheme.colors.textSecondary,
                style = PulseTheme.typography.footnote,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderOffsetPickerSheet(
    existingOffsets: Set<Int>,
    onDismiss: () -> Unit,
    onAdd: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var days by rememberSaveable { mutableIntStateOf(0) }
    var hours by rememberSaveable { mutableIntStateOf(1) }
    var minutes by rememberSaveable { mutableIntStateOf(0) }
    val totalMinutes = days * MINUTES_PER_DAY + hours * MINUTES_PER_HOUR + minutes
    val isDuplicate = totalMinutes in existingOffsets
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PulseTheme.colors.surface,
        contentColor = PulseTheme.colors.textPrimary,
        shape = RoundedCornerShape(
            topStart = PulseTheme.radius.xl,
            topEnd = PulseTheme.radius.xl,
        ),
        tonalElevation = PulseTheme.elevation.none,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = PulseTheme.spacing.sm)
                    .size(width = PulseTheme.spacing.xxxl, height = PulseTheme.spacing.xxs)
                    .background(
                        color = PulseTheme.colors.separator,
                        shape = RoundedCornerShape(PulseTheme.radius.full),
                    ),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = PulseTheme.spacing.xl,
                    end = PulseTheme.spacing.xl,
                    bottom = PulseTheme.spacing.xxxl,
                ),
            verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.settings_reminder_picker_title),
                color = PulseTheme.colors.textPrimary,
                style = PulseTheme.typography.title2,
            )
            Text(
                text = stringResource(R.string.settings_reminder_picker_body),
                color = PulseTheme.colors.textSecondary,
                style = PulseTheme.typography.footnote,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.sm),
            ) {
                ReminderWheel(
                    values = 0..MAX_REMINDER_DAYS,
                    initialValue = days,
                    unit = stringResource(R.string.settings_reminder_unit_day),
                    onValueChange = { days = it },
                    modifier = Modifier.weight(1f),
                )
                ReminderWheel(
                    values = 0..MAX_REMINDER_HOURS,
                    initialValue = hours,
                    unit = stringResource(R.string.settings_reminder_unit_hour),
                    onValueChange = { hours = it },
                    modifier = Modifier.weight(1f),
                )
                ReminderWheel(
                    values = 0..MAX_REMINDER_MINUTES,
                    initialValue = minutes,
                    unit = stringResource(R.string.settings_reminder_unit_minute),
                    onValueChange = { minutes = it },
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = if (isDuplicate) {
                    stringResource(R.string.settings_reminder_duplicate)
                } else {
                    stringResource(R.string.settings_reminder_picker_result, reminderOffsetLabel(totalMinutes))
                },
                color = if (isDuplicate) PulseTheme.colors.textSecondary else PulseTheme.colors.textPrimary,
                style = PulseTheme.typography.callout,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            ReminderPrimaryAction(
                label = stringResource(R.string.settings_reminder_confirm),
                enabled = !isDuplicate,
                onClick = { onAdd(totalMinutes) },
            )
        }
    }
}

@Composable
private fun ReminderWheel(
    values: IntRange,
    initialValue: Int,
    unit: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialIndex = (initialValue - values.first).coerceIn(0, values.count() - 1)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    LaunchedEffect(listState, values) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { item ->
                abs(item.offset + item.size / 2 - viewportCenter)
            }?.index
        }
            .distinctUntilChanged()
            .collect { index ->
                if (index != null) onValueChange(values.first + index)
            }
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = unit, color = PulseTheme.colors.textTertiary, style = PulseTheme.typography.caption1)
        Spacer(modifier = Modifier.height(PulseTheme.spacing.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PulseTheme.spacing.huge * WHEEL_VISIBLE_ITEM_COUNT),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PulseTheme.spacing.huge)
                    .background(
                        color = PulseTheme.colors.surfaceMuted,
                        shape = RoundedCornerShape(PulseTheme.radius.md),
                    ),
            )
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = PulseTheme.spacing.huge * 2),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(values.toList(), key = { it }) { value ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(PulseTheme.spacing.huge),
                    ) {
                        Text(
                            text = value.toString().padStart(2, '0'),
                            color = PulseTheme.colors.textPrimary,
                            style = PulseTheme.typography.title3,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderPrimaryAction(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (enabled) PulseTheme.colors.accent else PulseTheme.colors.separator,
                shape = RoundedCornerShape(PulseTheme.radius.md),
            )
            .pressEffect(contentDescription = label, enabled = enabled, onClick = onClick)
            .padding(PulseTheme.spacing.md),
    ) {
        Text(
            text = label,
            color = if (enabled) PulseTheme.colors.onAccent else PulseTheme.colors.textTertiary,
            style = PulseTheme.typography.footnote,
        )
    }
}

@Composable
private fun ReminderSystemAction(onClick: () -> Unit) {
    val label = stringResource(R.string.settings_open_system)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PulseTheme.colors.surface,
                shape = RoundedCornerShape(PulseTheme.radius.md),
            )
            .pressEffect(contentDescription = label, role = Role.Button, onClick = onClick)
            .padding(PulseTheme.spacing.md),
    ) {
        Text(text = label, color = PulseTheme.colors.textPrimary, style = PulseTheme.typography.footnote)
    }
}

@Composable
private fun reminderOffsetLabel(totalMinutes: Int): String {
    if (totalMinutes == 0) return stringResource(R.string.reminder_offset_start)
    val days = totalMinutes / MINUTES_PER_DAY
    val hours = totalMinutes % MINUTES_PER_DAY / MINUTES_PER_HOUR
    val minutes = totalMinutes % MINUTES_PER_HOUR
    return buildList {
        if (days > 0) add(stringResource(R.string.settings_reminder_days, days))
        if (hours > 0) add(stringResource(R.string.settings_reminder_hours, hours))
        if (minutes > 0) add(stringResource(R.string.settings_reminder_minutes, minutes))
    }.joinToString(" ")
}

private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
private const val MAX_REMINDER_DAYS = 30
private const val MAX_REMINDER_HOURS = 23
private const val MAX_REMINDER_MINUTES = 59
private const val WHEEL_VISIBLE_ITEM_COUNT = 5
