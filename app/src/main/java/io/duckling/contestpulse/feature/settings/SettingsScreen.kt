package io.duckling.contestpulse.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.app.NotificationManagerCompat
import io.duckling.contestpulse.R
import io.duckling.contestpulse.core.designsystem.component.appCard
import io.duckling.contestpulse.core.designsystem.component.pressEffect
import io.duckling.contestpulse.core.designsystem.component.topLevelScreenPadding
import io.duckling.contestpulse.core.designsystem.theme.PulseTheme
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.model.SourceSyncStatus
import io.duckling.contestpulse.feature.common.PageHeader
import io.duckling.contestpulse.feature.common.SelectableChip
import io.duckling.contestpulse.reminder.openReminderSystemSettings

@Composable
fun SettingsRoute(
    onOpenCustomSources: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationsEnabled = NotificationManagerCompat
        .from(context)
        .areNotificationsEnabled()
    SettingsScreen(
        uiState = uiState,
        notificationsEnabled = notificationsEnabled,
        onBackgroundSyncChange = viewModel::setBackgroundSyncEnabled,
        onWifiOnlyChange = viewModel::setWifiOnly,
        onIntervalChange = viewModel::setIntervalHours,
        onSourceEnabledChange = viewModel::setSourceEnabled,
        onOpenCustomSources = onOpenCustomSources,
        onOpenReminderSettings = {
            context.openReminderSystemSettings(
                notificationsEnabled = notificationsEnabled,
                exactRemindersAvailable = uiState.exactRemindersAvailable,
            )
        },
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    notificationsEnabled: Boolean,
    onBackgroundSyncChange: (Boolean) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onSourceEnabledChange: (ContestSource, Boolean) -> Unit,
    onOpenCustomSources: () -> Unit,
    onOpenReminderSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.statusBarsPadding(),
        contentPadding = topLevelScreenPadding(),
        verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.xxl),
    ) {
        item(key = "settings-header") {
            PageHeader(
                eyebrow = stringResource(R.string.settings_eyebrow),
                title = stringResource(R.string.settings_title),
                subtitle = stringResource(R.string.settings_subtitle),
            )
        }
        item(key = "settings-sources") {
            SettingsSectionTitle(text = stringResource(R.string.settings_sources_title))
            Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .appCard()
                    .padding(PulseTheme.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.lg),
            ) {
                CONFIGURABLE_SOURCES.forEach { source ->
                    val enabled = source in uiState.preferences.enabledSources
                    SettingsToggleRow(
                        label = sourceLabel(source),
                        supportingText = sourceSyncLabel(
                            enabled = enabled,
                            status = uiState.syncState.sources.firstOrNull {
                                status -> status.source == source
                            },
                        ),
                        checked = enabled,
                        onCheckedChange = { checked ->
                            onSourceEnabledChange(source, checked)
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
            SettingsCustomSourcesAction(
                count = uiState.customSourceCount,
                onClick = onOpenCustomSources,
            )
        }
        item(key = "settings-sync") {
            SettingsSectionTitle(text = stringResource(R.string.settings_sync_title))
            Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .appCard()
                    .padding(PulseTheme.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.lg),
            ) {
                SettingsToggleRow(
                    label = stringResource(R.string.settings_background_label),
                    supportingText = stringResource(R.string.settings_background_body),
                    checked = uiState.preferences.backgroundSyncEnabled,
                    onCheckedChange = onBackgroundSyncChange,
                )
                SettingsToggleRow(
                    label = stringResource(R.string.settings_wifi_label),
                    supportingText = stringResource(R.string.settings_wifi_body),
                    checked = uiState.preferences.wifiOnly,
                    enabled = uiState.preferences.backgroundSyncEnabled,
                    onCheckedChange = onWifiOnlyChange,
                )
                Text(
                    text = stringResource(R.string.settings_frequency_label),
                    color = PulseTheme.colors.textPrimary,
                    style = PulseTheme.typography.headline,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.sm),
                ) {
                    SYNC_INTERVALS.forEach { hours ->
                        SelectableChip(
                            label = stringResource(R.string.settings_frequency_hours, hours),
                            selected = uiState.preferences.intervalHours == hours,
                            onClick = { onIntervalChange(hours) },
                        )
                    }
                }
            }
        }
        item(key = "settings-reminders") {
            SettingsSectionTitle(text = stringResource(R.string.settings_reminders_title))
            Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .appCard()
                    .padding(PulseTheme.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.sm),
            ) {
                SettingsStatusRow(
                    label = stringResource(R.string.settings_notification_label),
                    status = stringResource(
                        if (notificationsEnabled) {
                            R.string.settings_status_available
                        } else {
                            R.string.settings_status_unavailable
                        },
                    ),
                )
                SettingsStatusRow(
                    label = stringResource(R.string.settings_exact_alarm_label),
                    status = stringResource(
                        if (uiState.exactRemindersAvailable) {
                            R.string.settings_status_exact
                        } else {
                            R.string.settings_status_inexact
                        },
                    ),
                )
                SettingsSystemAction(onClick = onOpenReminderSettings)
            }
        }
        item(key = "settings-privacy") {
            SettingsSectionTitle(text = stringResource(R.string.settings_privacy_title))
            Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .appCard()
                    .padding(PulseTheme.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.md),
            ) {
                PrivacyStatement(text = stringResource(R.string.settings_privacy_no_account))
                PrivacyStatement(text = stringResource(R.string.settings_privacy_local))
                PrivacyStatement(text = stringResource(R.string.settings_privacy_official))
                PrivacyStatement(text = stringResource(R.string.settings_privacy_affiliation))
            }
        }
    }
}

@Composable
private fun SettingsCustomSourcesAction(
    count: Int,
    onClick: () -> Unit,
) {
    val label = stringResource(R.string.settings_custom_sources_label)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appCard()
            .pressEffect(
                contentDescription = stringResource(R.string.settings_custom_sources_open),
                onClick = onClick,
            )
            .padding(PulseTheme.spacing.xl),
        horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = PulseTheme.colors.textPrimary,
                style = PulseTheme.typography.body,
            )
            Spacer(modifier = Modifier.height(PulseTheme.spacing.xxs))
            Text(
                text = if (count == 0) {
                    stringResource(R.string.settings_custom_sources_empty)
                } else {
                    stringResource(R.string.settings_custom_sources_count, count)
                },
                color = PulseTheme.colors.textTertiary,
                style = PulseTheme.typography.footnote,
            )
        }
        Text(
            text = "›",
            color = PulseTheme.colors.textPrimary,
            style = PulseTheme.typography.title3,
        )
    }
}

@Composable
private fun SettingsSystemAction(onClick: () -> Unit) {
    val label = stringResource(R.string.settings_open_system)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PulseTheme.colors.background,
                shape = RoundedCornerShape(PulseTheme.radius.md),
            )
            .pressEffect(
                contentDescription = label,
                onClick = onClick,
            )
            .padding(PulseTheme.spacing.md),
    ) {
        Text(
            text = label,
            color = PulseTheme.colors.textPrimary,
            style = PulseTheme.typography.footnote,
        )
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        color = PulseTheme.colors.textPrimary,
        style = PulseTheme.typography.title3,
    )
}

@Composable
private fun SettingsToggleRow(
    label: String,
    supportingText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressEffect(
                contentDescription = label,
                role = Role.Switch,
                enabled = enabled,
                onClick = { onCheckedChange(!checked) },
            ),
        horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = if (enabled) {
                    PulseTheme.colors.textPrimary
                } else {
                    PulseTheme.colors.textTertiary
                },
                style = PulseTheme.typography.body,
            )
            Spacer(modifier = Modifier.height(PulseTheme.spacing.xxs))
            Text(
                text = supportingText,
                color = PulseTheme.colors.textTertiary,
                style = PulseTheme.typography.footnote,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(PulseTheme.spacing.xxl)
                .background(
                    color = if (checked && enabled) {
                        PulseTheme.colors.accent
                    } else {
                        PulseTheme.colors.separator
                    },
                    shape = RoundedCornerShape(PulseTheme.radius.full),
                ),
        ) {
            Box(
                modifier = Modifier
                    .size(PulseTheme.spacing.sm)
                    .background(
                        color = if (checked && enabled) {
                            PulseTheme.colors.onAccent
                        } else {
                            PulseTheme.colors.textTertiary
                        },
                        shape = RoundedCornerShape(PulseTheme.radius.full),
                    ),
            )
        }
    }
}

@Composable
private fun SettingsStatusRow(
    label: String,
    status: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = PulseTheme.colors.textPrimary,
            style = PulseTheme.typography.body,
        )
        Text(
            text = status,
            color = PulseTheme.colors.textSecondary,
            style = PulseTheme.typography.footnote,
        )
    }
}

@Composable
private fun sourceLabel(source: ContestSource): String = stringResource(
    when (source) {
        ContestSource.CODEFORCES -> R.string.source_codeforces
        ContestSource.ATCODER -> R.string.source_atcoder
        ContestSource.LUOGU -> R.string.source_luogu
        ContestSource.NOWCODER -> R.string.source_nowcoder
        ContestSource.OTHER -> R.string.source_other
    },
)

@Composable
private fun sourceSyncLabel(
    enabled: Boolean,
    status: SourceSyncStatus?,
): String = when {
    !enabled -> stringResource(R.string.settings_source_disabled)
    status?.issue != null -> stringResource(R.string.settings_source_failed)
    status?.lastSuccessAt != null -> stringResource(
        R.string.settings_source_count,
        status.fetchedCount,
    )
    else -> stringResource(R.string.settings_source_waiting)
}

@Composable
private fun PrivacyStatement(text: String) {
    Text(
        text = "•  $text",
        color = PulseTheme.colors.textSecondary,
        style = PulseTheme.typography.callout,
    )
}

private val SYNC_INTERVALS = listOf(6, 12, 24)
private val CONFIGURABLE_SOURCES = listOf(
    ContestSource.CODEFORCES,
    ContestSource.ATCODER,
    ContestSource.LUOGU,
    ContestSource.NOWCODER,
)
