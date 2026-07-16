package io.duckling.contestpulse.feature.reminder

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import io.duckling.contestpulse.core.designsystem.component.pressEffect
import io.duckling.contestpulse.core.designsystem.theme.PulseTheme
import io.duckling.contestpulse.domain.logic.ReminderTriggerResult
import io.duckling.contestpulse.domain.logic.calculateReminderTrigger
import io.duckling.contestpulse.domain.logic.formatReminder
import io.duckling.contestpulse.domain.logic.hasDuplicateReminder
import io.duckling.contestpulse.domain.logic.validateReminderStructure
import io.duckling.contestpulse.domain.model.ReminderDeliveryStatus
import io.duckling.contestpulse.domain.model.ReminderProductConfig
import io.duckling.contestpulse.domain.model.ReminderRule
import io.duckling.contestpulse.domain.model.ReminderScheduleStatus
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

data class ReminderListItemModel(
    val id: String,
    val rule: ReminderRule,
    val scheduleStatus: ReminderScheduleStatus? = null,
    val deliveryStatus: ReminderDeliveryStatus = ReminderDeliveryStatus.NOT_ATTEMPTED,
    val isPreview: Boolean = false,
)

@Composable
fun ReminderList(
    reminders: List<ReminderListItemModel>,
    onEdit: (ReminderListItemModel) -> Unit,
    onDelete: (ReminderListItemModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.sm),
    ) {
        reminders.forEach { reminder ->
            key(reminder.id) {
                ReminderListItem(reminder, onEdit, onDelete)
            }
        }
    }
}

@Composable
fun ReminderListItem(
    reminder: ReminderListItemModel,
    onEdit: (ReminderListItemModel) -> Unit,
    onDelete: (ReminderListItemModel) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PulseTheme.colors.surface, RoundedCornerShape(PulseTheme.radius.md))
            .padding(start = PulseTheme.spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.xxs),
        ) {
            Text(
                text = formatReminder(reminder.rule),
                color = PulseTheme.colors.textPrimary,
                style = PulseTheme.typography.callout,
            )
            reminderStatusLabel(reminder)?.let { status ->
                Text(
                    text = status,
                    color = PulseTheme.colors.textTertiary,
                    style = PulseTheme.typography.caption1,
                )
            }
        }
        ReminderTextAction("编辑") { onEdit(reminder) }
        ReminderTextAction("删除") { onDelete(reminder) }
    }
}

@Composable
private fun ReminderTextAction(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(PulseTheme.spacing.giant)
            .pressEffect(label, role = Role.Button, onClick = onClick),
    ) {
        Text(label, color = PulseTheme.colors.textSecondary, style = PulseTheme.typography.footnote)
    }
}

private fun reminderStatusLabel(reminder: ReminderListItemModel): String? = when {
    reminder.isPreview -> "收藏比赛后生效"
    reminder.deliveryStatus == ReminderDeliveryStatus.DELIVERY_FAILED -> "通知未能送达"
    reminder.scheduleStatus == ReminderScheduleStatus.INVALID -> "提醒规则无效，请编辑"
    reminder.scheduleStatus == ReminderScheduleStatus.EXPIRED -> "提醒时间已过期"
    reminder.scheduleStatus == ReminderScheduleStatus.UNSCHEDULED -> "暂未注册系统提醒"
    else -> null
}

private enum class ReminderEditorStep { MAIN, SAME_DAY, LEGACY }
private enum class ReminderEditorMode { RELATIVE, FIXED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderDialog(
    visible: Boolean,
    editing: ReminderListItemModel?,
    existingRules: List<ReminderRule>,
    contestStart: Instant?,
    now: Instant,
    zoneId: ZoneId,
    onDismiss: () -> Unit,
    onSave: (ReminderRule) -> Unit,
) {
    val initialRule = editing?.rule
    val initialStep = when (initialRule) {
        is ReminderRule.Relative -> when {
            initialRule.offsetMinutes >= ReminderProductConfig.MINUTES_PER_DAY &&
                initialRule.offsetMinutes % ReminderProductConfig.MINUTES_PER_DAY != 0 ->
                ReminderEditorStep.LEGACY
            initialRule.offsetMinutes < ReminderProductConfig.MINUTES_PER_DAY ->
                ReminderEditorStep.SAME_DAY
            else -> ReminderEditorStep.MAIN
        }
        else -> ReminderEditorStep.MAIN
    }
    var step by remember(visible, editing?.id) {
        mutableStateOf<ReminderEditorStep?>(if (visible) initialStep else null)
    }
    var mode by remember(visible, editing?.id) {
        mutableStateOf(
            if (initialRule is ReminderRule.FixedTime) {
                ReminderEditorMode.FIXED
            } else {
                ReminderEditorMode.RELATIVE
            },
        )
    }
    var days by remember(visible, editing?.id) {
        mutableIntStateOf(
            when (initialRule) {
                is ReminderRule.Relative -> initialRule.offsetMinutes /
                    ReminderProductConfig.MINUTES_PER_DAY
                is ReminderRule.FixedTime -> initialRule.dayOffset
                null -> 0
            },
        )
    }
    var hours by remember(visible, editing?.id) {
        mutableIntStateOf(
            when (initialRule) {
                is ReminderRule.Relative -> initialRule.offsetMinutes %
                    ReminderProductConfig.MINUTES_PER_DAY /
                    ReminderProductConfig.MINUTES_PER_HOUR
                is ReminderRule.FixedTime -> initialRule.hour
                null -> 1
            },
        )
    }
    var minutes by remember(visible, editing?.id) {
        mutableIntStateOf(
            when (initialRule) {
                is ReminderRule.Relative -> initialRule.offsetMinutes %
                    ReminderProductConfig.MINUTES_PER_HOUR
                is ReminderRule.FixedTime -> initialRule.minute
                null -> 0
            },
        )
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun transitionTo(target: ReminderEditorStep) {
        scope.launch {
            sheetState.hide()
            step = null
            yield()
            step = target
        }
    }
    LaunchedEffect(visible, editing?.id) {
        if (!visible) step = null else if (step == null) step = initialStep
    }
    val currentStep = step ?: return
    val candidate = when (currentStep) {
        ReminderEditorStep.MAIN -> if (mode == ReminderEditorMode.FIXED) {
            ReminderRule.FixedTime(days, hours, minutes)
        } else {
            ReminderRule.Relative(days * ReminderProductConfig.MINUTES_PER_DAY)
        }
        ReminderEditorStep.SAME_DAY -> ReminderRule.Relative(
            hours * ReminderProductConfig.MINUTES_PER_HOUR + minutes,
        )
        ReminderEditorStep.LEGACY -> ReminderRule.Relative(
            days * ReminderProductConfig.MINUTES_PER_DAY +
                hours * ReminderProductConfig.MINUTES_PER_HOUR + minutes,
        )
    }
    val otherRules = existingRules.toMutableList().apply { initialRule?.let(::remove) }
    val validationMessage = validateCandidate(candidate, otherRules, contestStart, now, zoneId)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PulseTheme.colors.surface,
        contentColor = PulseTheme.colors.textPrimary,
        tonalElevation = PulseTheme.elevation.none,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        shape = RoundedCornerShape(topStart = PulseTheme.radius.xl, topEnd = PulseTheme.radius.xl),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = PulseTheme.spacing.xl,
                    end = PulseTheme.spacing.xl,
                    top = PulseTheme.spacing.lg,
                    bottom = PulseTheme.spacing.xxxl,
                ),
            verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (editing == null) "新增提醒" else "编辑提醒",
                    color = PulseTheme.colors.textPrimary,
                    style = PulseTheme.typography.title2,
                )
                if (currentStep == ReminderEditorStep.MAIN) {
                    ReminderModeSwitch(mode = mode, onModeChange = { mode = it })
                }
            }
            when (currentStep) {
                ReminderEditorStep.MAIN -> if (mode == ReminderEditorMode.RELATIVE) {
                    RelativeDayPicker(days = days, onDaysChange = { days = it })
                } else {
                    FixedDateTimePicker(
                        days = days,
                        hours = hours,
                        minutes = minutes,
                        onDaysChange = { days = it },
                        onHoursChange = { hours = it },
                        onMinutesChange = { minutes = it },
                    )
                }
                ReminderEditorStep.SAME_DAY -> RelativeTimePicker(
                    hours = hours,
                    minutes = minutes,
                    contestStart = contestStart,
                    now = now,
                    onHoursChange = { hours = it },
                    onMinutesChange = { minutes = it },
                )
                ReminderEditorStep.LEGACY -> LegacyRelativePicker(
                    days = days,
                    hours = hours,
                    minutes = minutes,
                    onDaysChange = { days = it },
                    onHoursChange = { hours = it },
                    onMinutesChange = { minutes = it },
                )
            }
            validationMessage?.let { message ->
                Text(
                    text = message,
                    color = PulseTheme.colors.textSecondary,
                    style = PulseTheme.typography.footnote,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (currentStep == ReminderEditorStep.SAME_DAY) {
                ReminderSecondaryAction("返回上一步") { transitionTo(ReminderEditorStep.MAIN) }
            }
            ReminderPrimaryAction(
                label = "保存提醒",
                enabled = validationMessage == null ||
                    (currentStep == ReminderEditorStep.MAIN &&
                        mode == ReminderEditorMode.RELATIVE && days == 0),
                onClick = {
                    if (currentStep == ReminderEditorStep.MAIN &&
                        mode == ReminderEditorMode.RELATIVE && days == 0
                    ) {
                        transitionTo(ReminderEditorStep.SAME_DAY)
                    } else {
                        onSave(candidate)
                    }
                },
            )
        }
    }
}

@Composable
private fun ReminderModeSwitch(
    mode: ReminderEditorMode,
    onModeChange: (ReminderEditorMode) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.xs)) {
        ReminderEditorMode.entries.forEach { item ->
            val label = if (item == ReminderEditorMode.RELATIVE) "比赛前" else "固定时间"
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = PulseTheme.spacing.giant + PulseTheme.spacing.lg, height = PulseTheme.spacing.giant)
                    .pressEffect(label, onClick = { onModeChange(item) }),
            ) {
                Text(
                    text = label,
                    color = if (mode == item) PulseTheme.colors.textPrimary else PulseTheme.colors.textTertiary,
                    style = PulseTheme.typography.footnote.copy(
                        fontWeight = if (mode == item) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                )
            }
        }
    }
}

@Composable
fun RelativeDayPicker(days: Int, onDaysChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        WheelWithUnit(
            values = (ReminderProductConfig.MIN_DAY_OFFSET..ReminderProductConfig.MAX_DAY_OFFSET).toList(),
            value = days,
            unit = "天前",
            padToTwoDigits = false,
            onValueChange = onDaysChange,
        )
        Spacer(Modifier.height(PulseTheme.spacing.sm))
        Text("0 表示比赛当天", color = PulseTheme.colors.textTertiary, style = PulseTheme.typography.caption1)
    }
}

@Composable
fun RelativeTimePicker(
    hours: Int,
    minutes: Int,
    contestStart: Instant?,
    now: Instant,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
) {
    val configuredMax = ReminderProductConfig.MAX_HOUR * ReminderProductConfig.MINUTES_PER_HOUR +
        ReminderProductConfig.MAX_MINUTE
    val maxTotal = contestStart?.let { start ->
        (Duration.between(now, start).toMinutes() - 1L)
            .coerceIn(0L, configuredMax.toLong())
            .toInt()
    } ?: configuredMax
    val maxHour = maxTotal / ReminderProductConfig.MINUTES_PER_HOUR
    val maxMinute = if (hours == maxHour) {
        maxTotal % ReminderProductConfig.MINUTES_PER_HOUR
    } else {
        ReminderProductConfig.MAX_MINUTE
    }
    LaunchedEffect(maxHour) { if (hours > maxHour) onHoursChange(maxHour) }
    LaunchedEffect(maxMinute) { if (minutes > maxMinute) onMinutesChange(maxMinute) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("比赛前", color = PulseTheme.colors.textSecondary, style = PulseTheme.typography.callout)
        Spacer(Modifier.width(PulseTheme.spacing.sm))
        WheelWithUnit(
            (ReminderProductConfig.MIN_HOUR..maxHour).toList(),
            hours,
            "小时",
            true,
            onHoursChange,
        )
        Spacer(Modifier.width(PulseTheme.spacing.sm))
        WheelWithUnit(
            (ReminderProductConfig.MIN_MINUTE..maxMinute step ReminderProductConfig.MINUTE_STEP).toList(),
            minutes,
            "分钟",
            true,
            onMinutesChange,
        )
    }
}

@Composable
fun FixedDateTimePicker(
    days: Int,
    hours: Int,
    minutes: Int,
    onDaysChange: (Int) -> Unit,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WheelWithUnit(
            (ReminderProductConfig.MIN_DAY_OFFSET..ReminderProductConfig.MAX_DAY_OFFSET).toList(),
            days,
            "天前",
            false,
            onDaysChange,
        )
        Spacer(Modifier.width(PulseTheme.spacing.xs))
        WheelWithUnit(
            (ReminderProductConfig.MIN_HOUR..ReminderProductConfig.MAX_HOUR).toList(),
            hours,
            "时",
            true,
            onHoursChange,
        )
        Spacer(Modifier.width(PulseTheme.spacing.xs))
        WheelWithUnit(
            (ReminderProductConfig.MIN_MINUTE..ReminderProductConfig.MAX_MINUTE step ReminderProductConfig.MINUTE_STEP).toList(),
            minutes,
            "分",
            true,
            onMinutesChange,
        )
    }
}

@Composable
private fun LegacyRelativePicker(
    days: Int,
    hours: Int,
    minutes: Int,
    onDaysChange: (Int) -> Unit,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("旧版复合提醒", color = PulseTheme.colors.textSecondary, style = PulseTheme.typography.footnote)
        FixedDateTimePicker(days, hours, minutes, onDaysChange, onHoursChange, onMinutesChange)
    }
}

@Composable
private fun WheelWithUnit(
    values: List<Int>,
    value: Int,
    unit: String,
    padToTwoDigits: Boolean,
    onValueChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        NumberWheelPicker(values, value, padToTwoDigits, onValueChange)
        Spacer(Modifier.width(PulseTheme.spacing.xxs))
        Text(
            text = unit,
            color = PulseTheme.colors.textSecondary,
            style = PulseTheme.typography.footnote,
            maxLines = 1,
        )
    }
}

@Composable
fun NumberWheelPicker(
    values: List<Int>,
    value: Int,
    padToTwoDigits: Boolean,
    onValueChange: (Int) -> Unit,
) {
    val safeValues = values.ifEmpty { listOf(0) }
    val initialIndex = safeValues.indexOf(value).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    var selectedValue by remember(value, safeValues) { mutableIntStateOf(safeValues[initialIndex]) }
    LaunchedEffect(value, safeValues) {
        val target = safeValues.indexOf(value).takeIf { it >= 0 } ?: 0
        listState.scrollToItem(target)
        selectedValue = safeValues[target]
    }
    LaunchedEffect(listState, safeValues) {
        snapshotFlow {
            val info = listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { item ->
                abs(item.offset + item.size / 2 - center)
            }?.index
        }.distinctUntilChanged().collect { index ->
            index?.let {
                selectedValue = safeValues[it]
                onValueChange(selectedValue)
            }
        }
    }
    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = PulseTheme.spacing.huge * 2),
        modifier = Modifier
            .width(PulseTheme.spacing.giant)
            .height(PulseTheme.spacing.huge * 5),
    ) {
        items(safeValues, key = { it }) { item ->
            val distance = abs(safeValues.indexOf(item) - safeValues.indexOf(selectedValue))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PulseTheme.spacing.huge),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (padToTwoDigits) item.toString().padStart(2, '0') else item.toString(),
                    color = if (distance == 0) {
                        PulseTheme.colors.textPrimary
                    } else {
                        PulseTheme.colors.textSecondary.copy(alpha = (0.66f - distance * 0.14f).coerceAtLeast(0.24f))
                    },
                    style = if (distance == 0) {
                        PulseTheme.typography.title3.copy(fontWeight = FontWeight.Bold)
                    } else {
                        PulseTheme.typography.body
                    },
                )
            }
        }
    }
}

private fun validateCandidate(
    candidate: ReminderRule,
    existingRules: List<ReminderRule>,
    contestStart: Instant?,
    now: Instant,
    zoneId: ZoneId,
): String? {
    if (hasDuplicateReminder(candidate, existingRules, contestStart, now, zoneId)) {
        return "这个提醒已经存在"
    }
    validateReminderStructure(candidate)?.let {
        return if (it.name == "ZERO_OFFSET") "小时和分钟不能同时为 0" else "提醒数值超出范围"
    }
    if (contestStart != null) {
        return when (val result = calculateReminderTrigger(contestStart, candidate, now, zoneId)) {
            is ReminderTriggerResult.Valid -> null
            is ReminderTriggerResult.Invalid -> when (result.error) {
                io.duckling.contestpulse.domain.logic.ReminderValidationError.NOT_BEFORE_CONTEST ->
                    "提醒时间必须早于比赛开始时间"
                io.duckling.contestpulse.domain.logic.ReminderValidationError.NOT_IN_FUTURE ->
                    "提醒时间已经过去"
                io.duckling.contestpulse.domain.logic.ReminderValidationError.NONEXISTENT_LOCAL_TIME ->
                    "当前时区不存在这个本地时间"
                else -> "提醒设置无效"
            }
        }
    }
    return null
}

@Composable
private fun ReminderPrimaryAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (enabled) PulseTheme.colors.accent else PulseTheme.colors.separator,
                RoundedCornerShape(PulseTheme.radius.md),
            )
            .pressEffect(label, enabled = enabled, onClick = onClick)
            .padding(PulseTheme.spacing.md),
    ) {
        Text(
            label,
            color = if (enabled) PulseTheme.colors.onAccent else PulseTheme.colors.textTertiary,
            style = PulseTheme.typography.footnote,
        )
    }
}

@Composable
private fun ReminderSecondaryAction(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .pressEffect(label, onClick = onClick)
            .padding(PulseTheme.spacing.sm),
    ) {
        Text(label, color = PulseTheme.colors.textSecondary, style = PulseTheme.typography.footnote)
    }
}
