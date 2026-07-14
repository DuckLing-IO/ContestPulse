package io.duckling.contestpulse.feature.customsource

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.duckling.contestpulse.R
import io.duckling.contestpulse.core.designsystem.component.FadeTransition
import io.duckling.contestpulse.core.designsystem.component.appCard
import io.duckling.contestpulse.core.designsystem.component.pressEffect
import io.duckling.contestpulse.core.designsystem.theme.PulseTheme
import io.duckling.contestpulse.domain.customsource.CustomContestSource
import io.duckling.contestpulse.domain.customsource.CustomSourceFormat
import io.duckling.contestpulse.domain.model.SourceSyncStatus
import io.duckling.contestpulse.feature.common.PageHeader
import io.duckling.contestpulse.feature.common.SelectableChip
import io.duckling.contestpulse.feature.common.localDateTimeLabel

@Composable
fun CustomSourcesRoute(
    onBack: () -> Unit,
    viewModel: CustomSourcesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CustomSourcesScreen(
        uiState = uiState,
        onBack = {
            if (uiState.editor != null) viewModel.closeEditor() else onBack()
        },
        onAdd = viewModel::startAdding,
        onEdit = viewModel::startEditing,
        onSetEnabled = viewModel::setEnabled,
        onRequestDelete = viewModel::requestDelete,
        onCancelDelete = viewModel::cancelDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onNameChange = viewModel::updateName,
        onUrlChange = viewModel::updateUrl,
        onTimezoneChange = viewModel::updateTimezone,
        onFormatChange = viewModel::selectFormat,
        onToggleAdvanced = viewModel::toggleAdvanced,
        onItemSelectorChange = viewModel::updateItemSelector,
        onTitleSelectorChange = viewModel::updateTitleSelector,
        onStartSelectorChange = viewModel::updateStartSelector,
        onEndSelectorChange = viewModel::updateEndSelector,
        onLinkSelectorChange = viewModel::updateLinkSelector,
        onDateTimePatternChange = viewModel::updateDateTimePattern,
        onPreview = viewModel::preview,
        onSave = viewModel::save,
    )
}

@Composable
fun CustomSourcesScreen(
    uiState: CustomSourcesUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (CustomContestSource) -> Unit,
    onSetEnabled: (CustomContestSource, Boolean) -> Unit,
    onRequestDelete: (String) -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: (CustomContestSource) -> Unit,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onTimezoneChange: (String) -> Unit,
    onFormatChange: (CustomSourceFormat) -> Unit,
    onToggleAdvanced: () -> Unit,
    onItemSelectorChange: (String) -> Unit,
    onTitleSelectorChange: (String) -> Unit,
    onStartSelectorChange: (String) -> Unit,
    onEndSelectorChange: (String) -> Unit,
    onLinkSelectorChange: (String) -> Unit,
    onDateTimePatternChange: (String) -> Unit,
    onPreview: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PulseTheme.colors.background)
            .statusBarsPadding()
            .imePadding(),
    ) {
        FadeTransition(
            visible = uiState.editor == null,
            modifier = Modifier.fillMaxSize(),
        ) {
            CustomSourceList(
                uiState = uiState,
                onBack = onBack,
                onAdd = onAdd,
                onEdit = onEdit,
                onSetEnabled = onSetEnabled,
                onRequestDelete = onRequestDelete,
                onCancelDelete = onCancelDelete,
                onConfirmDelete = onConfirmDelete,
            )
        }
        FadeTransition(
            visible = uiState.editor != null,
            modifier = Modifier.fillMaxSize(),
        ) {
            uiState.editor?.let { editor ->
                CustomSourceEditor(
                    state = editor,
                    onBack = onBack,
                    onNameChange = onNameChange,
                    onUrlChange = onUrlChange,
                    onTimezoneChange = onTimezoneChange,
                    onFormatChange = onFormatChange,
                    onToggleAdvanced = onToggleAdvanced,
                    onItemSelectorChange = onItemSelectorChange,
                    onTitleSelectorChange = onTitleSelectorChange,
                    onStartSelectorChange = onStartSelectorChange,
                    onEndSelectorChange = onEndSelectorChange,
                    onLinkSelectorChange = onLinkSelectorChange,
                    onDateTimePatternChange = onDateTimePatternChange,
                    onPreview = onPreview,
                    onSave = onSave,
                )
            }
        }
    }
}

@Composable
private fun CustomSourceList(
    uiState: CustomSourcesUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (CustomContestSource) -> Unit,
    onSetEnabled: (CustomContestSource, Boolean) -> Unit,
    onRequestDelete: (String) -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: (CustomContestSource) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = PulseTheme.spacing.xl,
            top = PulseTheme.spacing.md,
            end = PulseTheme.spacing.xl,
            bottom = PulseTheme.spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.xl),
    ) {
        item(key = "custom-source-header") {
            BackAction(onBack)
            Spacer(modifier = Modifier.height(PulseTheme.spacing.xl))
            PageHeader(
                eyebrow = stringResource(R.string.custom_sources_eyebrow),
                title = stringResource(R.string.custom_sources_title),
                subtitle = stringResource(R.string.custom_sources_subtitle),
            )
        }

        if (uiState.operationError != null) {
            item(key = "custom-source-operation-error") {
                Text(
                    text = uiState.operationError.label(),
                    color = PulseTheme.colors.textPrimary,
                    style = PulseTheme.typography.callout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            PulseTheme.colors.surfaceMuted,
                            RoundedCornerShape(PulseTheme.radius.md),
                        )
                        .padding(PulseTheme.spacing.md),
                )
            }
        }

        if (uiState.sources.isEmpty()) {
            item(key = "custom-source-empty") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appCard()
                        .padding(PulseTheme.spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.sm),
                ) {
                    Text(
                        text = stringResource(R.string.custom_sources_empty_title),
                        color = PulseTheme.colors.textPrimary,
                        style = PulseTheme.typography.title3,
                    )
                    Text(
                        text = stringResource(R.string.custom_sources_empty_body),
                        color = PulseTheme.colors.textSecondary,
                        style = PulseTheme.typography.callout,
                    )
                }
            }
        } else {
            items(
                count = uiState.sources.size,
                key = { index -> uiState.sources[index].source.id },
            ) { index ->
                val item = uiState.sources[index]
                CustomSourceCard(
                    item = item,
                    isDeletePending = uiState.pendingDeleteId == item.source.id,
                    onEdit = { onEdit(item.source) },
                    onSetEnabled = { enabled -> onSetEnabled(item.source, enabled) },
                    onRequestDelete = { onRequestDelete(item.source.id) },
                    onCancelDelete = onCancelDelete,
                    onConfirmDelete = { onConfirmDelete(item.source) },
                )
            }
        }

        item(key = "custom-source-add") {
            PrimaryAction(
                label = stringResource(R.string.custom_sources_add),
                enabled = true,
                onClick = onAdd,
            )
            Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
            Text(
                text = stringResource(R.string.custom_sources_security_note),
                color = PulseTheme.colors.textTertiary,
                style = PulseTheme.typography.footnote,
            )
        }
    }
}

@Composable
private fun CustomSourceCard(
    item: CustomSourceListItem,
    isDeletePending: Boolean,
    onEdit: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
    onRequestDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    val source = item.source
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appCard()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy, // keep confirmation expansion calm
                    stiffness = Spring.StiffnessMedium, // respond without lingering
                ),
            )
            .padding(PulseTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    color = PulseTheme.colors.textPrimary,
                    style = PulseTheme.typography.headline,
                )
                Spacer(modifier = Modifier.height(PulseTheme.spacing.xxs))
                Text(
                    text = customSourceStatus(source, item.syncStatus),
                    color = PulseTheme.colors.textTertiary,
                    style = PulseTheme.typography.footnote,
                )
            }
            SourceToggle(
                checked = source.enabled,
                label = source.name,
                onCheckedChange = onSetEnabled,
            )
        }
        Text(
            text = source.url,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = PulseTheme.colors.textSecondary,
            style = PulseTheme.typography.footnote,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.sm),
        ) {
            TextAction(
                label = stringResource(R.string.custom_source_edit),
                onClick = onEdit,
                modifier = Modifier.weight(1f),
            )
            TextAction(
                label = stringResource(R.string.custom_source_delete),
                onClick = onRequestDelete,
                modifier = Modifier.weight(1f),
            )
        }
        FadeTransition(visible = isDeletePending) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        PulseTheme.colors.surface,
                        RoundedCornerShape(PulseTheme.radius.md),
                    )
                    .padding(PulseTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.custom_source_delete_body),
                    color = PulseTheme.colors.textPrimary,
                    style = PulseTheme.typography.footnote,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.sm),
                ) {
                    TextAction(
                        label = stringResource(R.string.custom_source_cancel),
                        onClick = onCancelDelete,
                        modifier = Modifier.weight(1f),
                    )
                    TextAction(
                        label = stringResource(R.string.custom_source_delete_confirm),
                        onClick = onConfirmDelete,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomSourceEditor(
    state: CustomSourceEditorState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onTimezoneChange: (String) -> Unit,
    onFormatChange: (CustomSourceFormat) -> Unit,
    onToggleAdvanced: () -> Unit,
    onItemSelectorChange: (String) -> Unit,
    onTitleSelectorChange: (String) -> Unit,
    onStartSelectorChange: (String) -> Unit,
    onEndSelectorChange: (String) -> Unit,
    onLinkSelectorChange: (String) -> Unit,
    onDateTimePatternChange: (String) -> Unit,
    onPreview: () -> Unit,
    onSave: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = PulseTheme.spacing.xl,
            top = PulseTheme.spacing.md,
            end = PulseTheme.spacing.xl,
            bottom = PulseTheme.spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.xl),
    ) {
        item(key = "editor-header") {
            BackAction(onBack)
            Spacer(modifier = Modifier.height(PulseTheme.spacing.xl))
            PageHeader(
                eyebrow = stringResource(R.string.custom_source_editor_eyebrow),
                title = stringResource(R.string.custom_source_editor_title),
                subtitle = stringResource(R.string.custom_source_editor_subtitle),
            )
        }
        item(key = "editor-basics") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .appCard()
                    .padding(PulseTheme.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.md),
            ) {
                AppTextField(
                    label = stringResource(R.string.custom_source_name_label),
                    value = state.name,
                    placeholder = stringResource(R.string.custom_source_name_hint),
                    onValueChange = onNameChange,
                )
                AppTextField(
                    label = stringResource(R.string.custom_source_url_label),
                    value = state.url,
                    placeholder = stringResource(R.string.custom_source_url_hint),
                    onValueChange = onUrlChange,
                )
                AppTextField(
                    label = stringResource(R.string.custom_source_timezone_label),
                    value = state.timezoneId,
                    placeholder = stringResource(R.string.custom_source_timezone_hint),
                    onValueChange = onTimezoneChange,
                )
            }
        }
        item(key = "editor-format") {
            Text(
                text = stringResource(R.string.custom_source_format_title),
                color = PulseTheme.colors.textPrimary,
                style = PulseTheme.typography.title3,
            )
            Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.sm),
            ) {
                CustomSourceFormat.entries.forEach { format ->
                    SelectableChip(
                        label = format.label(),
                        selected = state.format == format,
                        onClick = { onFormatChange(format) },
                    )
                }
            }
        }
        item(key = "editor-advanced") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .appCard()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy, // settle field expansion cleanly
                            stiffness = Spring.StiffnessMedium, // keep progressive disclosure responsive
                        ),
                    )
                    .padding(PulseTheme.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.md),
            ) {
                TextAction(
                    label = stringResource(
                        if (state.isAdvancedExpanded) {
                            R.string.custom_source_advanced_collapse
                        } else {
                            R.string.custom_source_advanced_expand
                        },
                    ),
                    onClick = onToggleAdvanced,
                )
                Text(
                    text = stringResource(R.string.custom_source_advanced_body),
                    color = PulseTheme.colors.textTertiary,
                    style = PulseTheme.typography.footnote,
                )
                FadeTransition(visible = state.isAdvancedExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.md)) {
                        AppTextField(
                            label = stringResource(R.string.custom_source_item_selector),
                            value = state.itemSelector,
                            placeholder = ".contest-item",
                            onValueChange = onItemSelectorChange,
                        )
                        AppTextField(
                            label = stringResource(R.string.custom_source_title_selector),
                            value = state.titleSelector,
                            placeholder = ".title",
                            onValueChange = onTitleSelectorChange,
                        )
                        AppTextField(
                            label = stringResource(R.string.custom_source_start_selector),
                            value = state.startSelector,
                            placeholder = "time.start",
                            onValueChange = onStartSelectorChange,
                        )
                        AppTextField(
                            label = stringResource(R.string.custom_source_end_selector),
                            value = state.endSelector,
                            placeholder = "time.end",
                            onValueChange = onEndSelectorChange,
                        )
                        AppTextField(
                            label = stringResource(R.string.custom_source_link_selector),
                            value = state.linkSelector,
                            placeholder = "a.contest-link",
                            onValueChange = onLinkSelectorChange,
                        )
                        AppTextField(
                            label = stringResource(R.string.custom_source_pattern_label),
                            value = state.dateTimePattern,
                            placeholder = "yyyy-MM-dd HH:mm",
                            onValueChange = onDateTimePatternChange,
                        )
                    }
                }
            }
        }
        item(key = "editor-error") {
            FadeTransition(visible = state.error != null) {
                state.error?.let { error ->
                    Text(
                        text = error.label(),
                        color = PulseTheme.colors.textPrimary,
                        style = PulseTheme.typography.callout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                PulseTheme.colors.surfaceMuted,
                                RoundedCornerShape(PulseTheme.radius.md),
                            )
                            .padding(PulseTheme.spacing.md),
                    )
                }
            }
        }
        item(key = "editor-preview") {
            FadeTransition(visible = state.preview != null) {
                state.preview?.let { preview -> PreviewCard(preview) }
            }
        }
        item(key = "editor-actions") {
            if (state.preview == null) {
                PrimaryAction(
                    label = stringResource(
                        if (state.isPreviewing) {
                            R.string.custom_source_previewing
                        } else {
                            R.string.custom_source_preview_action
                        },
                    ),
                    enabled = !state.isPreviewing,
                    onClick = onPreview,
                )
            } else {
                PrimaryAction(
                    label = stringResource(R.string.custom_source_save_action),
                    enabled = !state.isPreviewing,
                    onClick = onSave,
                )
                Spacer(modifier = Modifier.height(PulseTheme.spacing.sm))
                TextAction(
                    label = stringResource(R.string.custom_source_preview_again),
                    onClick = onPreview,
                )
            }
            Spacer(modifier = Modifier.height(PulseTheme.spacing.md))
            Text(
                text = stringResource(R.string.custom_source_permission_note),
                color = PulseTheme.colors.textTertiary,
                style = PulseTheme.typography.footnote,
            )
        }
    }
}

@Composable
private fun PreviewCard(preview: CustomSourcePreviewUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appCard()
            .padding(PulseTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.md),
    ) {
        Text(
            text = stringResource(R.string.custom_source_preview_title),
            color = PulseTheme.colors.textPrimary,
            style = PulseTheme.typography.title3,
        )
        Text(
            text = stringResource(
                R.string.custom_source_preview_summary,
                preview.detectedFormat.label(),
                preview.contests.size,
            ),
            color = PulseTheme.colors.textSecondary,
            style = PulseTheme.typography.footnote,
        )
        preview.contests.take(PREVIEW_ITEM_LIMIT).forEach { contest ->
            Column(verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.xxs)) {
                Text(
                    text = contest.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = PulseTheme.colors.textPrimary,
                    style = PulseTheme.typography.callout,
                )
                Text(
                    text = contest.startTime.localDateTimeLabel(),
                    color = PulseTheme.colors.textTertiary,
                    style = PulseTheme.typography.footnote,
                )
            }
        }
        preview.warnings.forEach { warning ->
            Text(
                text = warning,
                color = PulseTheme.colors.textTertiary,
                style = PulseTheme.typography.footnote,
            )
        }
    }
}

@Composable
private fun AppTextField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PulseTheme.colors.surface, RoundedCornerShape(PulseTheme.radius.md))
            .padding(PulseTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.xs),
    ) {
        Text(
            text = label,
            color = PulseTheme.colors.textTertiary,
            style = PulseTheme.typography.caption1,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = PulseTheme.typography.body.copy(color = PulseTheme.colors.textPrimary),
            cursorBrush = SolidColor(PulseTheme.colors.textPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(PulseTheme.spacing.huge)
                .semantics { contentDescription = label },
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = PulseTheme.colors.textTertiary,
                            style = PulseTheme.typography.body,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun BackAction(onClick: () -> Unit) {
    val label = stringResource(R.string.detail_back)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(PulseTheme.spacing.huge)
            .background(PulseTheme.colors.surfaceMuted, RoundedCornerShape(PulseTheme.radius.full))
            .pressEffect(contentDescription = label, onClick = onClick),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_back),
            contentDescription = null,
            tint = PulseTheme.colors.textPrimary,
            modifier = Modifier.size(PulseTheme.spacing.xl),
        )
    }
}

@Composable
private fun PrimaryAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (enabled) PulseTheme.colors.accent else PulseTheme.colors.separator,
                RoundedCornerShape(PulseTheme.radius.lg),
            )
            .pressEffect(
                contentDescription = label,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(PulseTheme.spacing.md),
    ) {
        Text(
            text = label,
            color = if (enabled) PulseTheme.colors.onAccent else PulseTheme.colors.textTertiary,
            style = PulseTheme.typography.headline,
        )
    }
}

@Composable
private fun TextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(PulseTheme.colors.surface, RoundedCornerShape(PulseTheme.radius.md))
            .pressEffect(contentDescription = label, onClick = onClick)
            .padding(horizontal = PulseTheme.spacing.md, vertical = PulseTheme.spacing.sm),
    ) {
        Text(
            text = label,
            color = PulseTheme.colors.textPrimary,
            style = PulseTheme.typography.footnote,
        )
    }
}

@Composable
private fun SourceToggle(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(PulseTheme.spacing.huge)
            .pressEffect(
                contentDescription = stringResource(
                    R.string.custom_source_toggle_description,
                    label,
                ),
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) },
            ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(PulseTheme.spacing.xxl)
                .background(
                    if (checked) PulseTheme.colors.accent else PulseTheme.colors.separator,
                    RoundedCornerShape(PulseTheme.radius.full),
                ),
        ) {
            Box(
                modifier = Modifier
                    .size(PulseTheme.spacing.xs)
                    .background(
                        if (checked) PulseTheme.colors.onAccent else PulseTheme.colors.textTertiary,
                        RoundedCornerShape(PulseTheme.radius.full),
                    ),
            )
        }
    }
}

@Composable
private fun customSourceStatus(
    source: CustomContestSource,
    status: SourceSyncStatus?,
): String = when {
    !source.enabled -> stringResource(R.string.settings_source_disabled)
    status?.issue != null -> stringResource(R.string.settings_source_failed)
    status?.lastSuccessAt != null -> stringResource(R.string.settings_source_count, status.fetchedCount)
    else -> stringResource(R.string.settings_source_waiting)
}

@Composable
private fun CustomSourceFormat.label(): String = stringResource(
    when (this) {
        CustomSourceFormat.AUTO -> R.string.custom_source_format_auto
        CustomSourceFormat.JSON -> R.string.custom_source_format_json
        CustomSourceFormat.ICALENDAR -> R.string.custom_source_format_calendar
        CustomSourceFormat.HTML -> R.string.custom_source_format_html
    },
)

@Composable
private fun CustomSourceEditorError.label(): String = stringResource(
    when (this) {
        CustomSourceEditorError.INVALID_INPUT -> R.string.custom_source_error_input
        CustomSourceEditorError.NETWORK -> R.string.custom_source_error_network
        CustomSourceEditorError.HTTP -> R.string.custom_source_error_http
        CustomSourceEditorError.PARSING -> R.string.custom_source_error_parsing
        CustomSourceEditorError.REMOTE -> R.string.custom_source_error_remote
        CustomSourceEditorError.UNKNOWN -> R.string.custom_source_error_unknown
    },
)

private const val PREVIEW_ITEM_LIMIT = 5
