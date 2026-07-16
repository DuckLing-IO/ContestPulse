package io.duckling.contestpulse.feature.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import io.duckling.contestpulse.core.designsystem.component.FadeTransition
import io.duckling.contestpulse.core.designsystem.component.appCard
import io.duckling.contestpulse.core.designsystem.component.pressEffect
import io.duckling.contestpulse.core.designsystem.theme.PulseTheme
import io.duckling.contestpulse.domain.logic.validateReminderStructure
import io.duckling.contestpulse.domain.model.ReminderDefinition
import io.duckling.contestpulse.domain.model.ReminderScheduleStatus
import io.duckling.contestpulse.feature.reminder.AddReminderDialog
import io.duckling.contestpulse.feature.reminder.ReminderList
import io.duckling.contestpulse.feature.reminder.ReminderListItemModel
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@Composable
internal fun DefaultReminderSettingsCard(
    reminders: List<ReminderDefinition>,
    notificationsEnabled: Boolean,
    exactRemindersAvailable: Boolean,
    onRemindersChange: (List<ReminderDefinition>) -> Unit,
    onOpenSystemSettings: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val items = reminders.map { definition ->
        ReminderListItemModel(
            id = definition.id,
            rule = definition.rule,
            scheduleStatus = if (validateReminderStructure(definition.rule) == null) {
                null
            } else {
                ReminderScheduleStatus.INVALID
            },
        )
    }
    val editing = items.firstOrNull { it.id == editingId }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appCard()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy, // settle without overshoot
                    stiffness = Spring.StiffnessMedium, // responsive disclosure
                ),
            )
            .padding(PulseTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.md),
    ) {
        ReminderCapabilityRow("通知权限", if (notificationsEnabled) "可用" else "未开放")
        ReminderCapabilityRow("精确闹钟", if (exactRemindersAvailable) "精确" else "可能延迟")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressEffect(
                    if (expanded) "收起默认提醒" else "展开默认提醒",
                    onClick = { expanded = !expanded },
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.xxs)) {
                Text("默认提醒时间设置", color = PulseTheme.colors.textPrimary, style = PulseTheme.typography.headline)
                Text(
                    if (reminders.isEmpty()) "未设置自动提醒" else "已设置 ${reminders.size} 个提醒",
                    color = PulseTheme.colors.textTertiary,
                    style = PulseTheme.typography.footnote,
                )
            }
            Text(if (expanded) "收起" else "展开", color = PulseTheme.colors.textSecondary, style = PulseTheme.typography.footnote)
        }
        FadeTransition(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.sm)) {
                Text(
                    "收藏比赛时复制这些提醒；修改只影响之后收藏的比赛。",
                    color = PulseTheme.colors.textTertiary,
                    style = PulseTheme.typography.footnote,
                )
                if (items.isEmpty()) {
                    Text("新收藏的比赛将不会自动创建提醒。", color = PulseTheme.colors.textSecondary, style = PulseTheme.typography.callout)
                } else {
                    ReminderList(
                        reminders = items,
                        onEdit = {
                            editingId = it.id
                            showEditor = true
                        },
                        onDelete = { item ->
                            onRemindersChange(reminders.filterNot { it.id == item.id })
                        },
                    )
                }
                PrimaryAction("新增提醒") {
                    editingId = null
                    showEditor = true
                }
            }
        }
        SystemAction(onOpenSystemSettings)
    }
    AddReminderDialog(
        visible = showEditor,
        editing = editing,
        existingRules = reminders.map(ReminderDefinition::rule),
        contestStart = null,
        now = Instant.now(),
        zoneId = ZoneId.systemDefault(),
        onDismiss = { showEditor = false },
        onSave = { rule ->
            val existing = reminders.firstOrNull { it.id == editingId }
            val updated = if (existing == null) {
                reminders + ReminderDefinition(UUID.randomUUID().toString(), rule, Instant.now())
            } else {
                reminders.map { if (it.id == existing.id) existing.copy(rule = rule) else it }
            }
            onRemindersChange(updated)
            showEditor = false
        },
    )
}

@Composable
private fun ReminderCapabilityRow(label: String, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = PulseTheme.colors.textPrimary, style = PulseTheme.typography.callout)
        Text(status, color = PulseTheme.colors.textSecondary, style = PulseTheme.typography.footnote)
    }
}

@Composable
private fun PrimaryAction(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(PulseTheme.colors.accent, RoundedCornerShape(PulseTheme.radius.md))
            .pressEffect(label, onClick = onClick)
            .padding(PulseTheme.spacing.md),
    ) {
        Text(label, color = PulseTheme.colors.onAccent, style = PulseTheme.typography.footnote)
    }
}

@Composable
private fun SystemAction(onClick: () -> Unit) {
    val label = "打开系统提醒设置"
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(PulseTheme.colors.surface, RoundedCornerShape(PulseTheme.radius.md))
            .pressEffect(label, role = Role.Button, onClick = onClick)
            .padding(PulseTheme.spacing.md),
    ) {
        Text(label, color = PulseTheme.colors.textPrimary, style = PulseTheme.typography.footnote)
    }
}
