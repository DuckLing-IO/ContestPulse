package io.duckling.contestpulse.feature.customsource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.duckling.contestpulse.data.remote.RemoteApiException
import io.duckling.contestpulse.data.remote.RemoteHttpException
import io.duckling.contestpulse.data.remote.RemoteParsingException
import io.duckling.contestpulse.data.remote.custom.CustomSourceParseResult
import io.duckling.contestpulse.data.remote.custom.CustomSourcePreviewer
import io.duckling.contestpulse.domain.customsource.CustomContestSource
import io.duckling.contestpulse.domain.customsource.CustomHtmlSelectors
import io.duckling.contestpulse.domain.customsource.CustomSourceFormat
import io.duckling.contestpulse.domain.customsource.CustomSourceRepository
import io.duckling.contestpulse.domain.customsource.DEFAULT_CUSTOM_SOURCE_TIMEZONE
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestSyncState
import io.duckling.contestpulse.domain.model.SourceSyncStatus
import io.duckling.contestpulse.domain.repository.ContestRepository
import java.io.IOException
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomSourcesUiState(
    val isLoading: Boolean = true,
    val sources: List<CustomSourceListItem> = emptyList(),
    val editor: CustomSourceEditorState? = null,
    val pendingDeleteId: String? = null,
    val operationError: CustomSourceEditorError? = null,
)

data class CustomSourceListItem(
    val source: CustomContestSource,
    val syncStatus: SourceSyncStatus?,
)

data class CustomSourceEditorState(
    val id: String,
    val name: String = "",
    val url: String = "",
    val timezoneId: String = DEFAULT_CUSTOM_SOURCE_TIMEZONE,
    val format: CustomSourceFormat = CustomSourceFormat.AUTO,
    val itemSelector: String = "",
    val titleSelector: String = "",
    val startSelector: String = "",
    val endSelector: String = "",
    val linkSelector: String = "",
    val dateTimePattern: String = "",
    val isAdvancedExpanded: Boolean = false,
    val isPreviewing: Boolean = false,
    val preview: CustomSourcePreviewUi? = null,
    val error: CustomSourceEditorError? = null,
)

data class CustomSourcePreviewUi(
    val detectedFormat: CustomSourceFormat,
    val contests: List<Contest>,
    val warnings: List<String>,
)

enum class CustomSourceEditorError {
    INVALID_INPUT,
    NETWORK,
    HTTP,
    PARSING,
    REMOTE,
    UNKNOWN,
}

private data class CustomSourceRepositoryState(
    val sources: List<CustomContestSource>,
    val syncState: ContestSyncState,
)

@HiltViewModel
class CustomSourcesViewModel @Inject constructor(
    private val customSourceRepository: CustomSourceRepository,
    private val previewer: CustomSourcePreviewer,
    private val contestRepository: ContestRepository,
) : ViewModel() {
    private val editor = MutableStateFlow<CustomSourceEditorState?>(null)
    private val pendingDeleteId = MutableStateFlow<String?>(null)
    private val operationError = MutableStateFlow<CustomSourceEditorError?>(null)
    private val repositoryState = combine(
        customSourceRepository.sources,
        contestRepository.observeSyncState(),
    ) { sources, syncState -> CustomSourceRepositoryState(sources, syncState) }

    val uiState: StateFlow<CustomSourcesUiState> = combine(
        repositoryState,
        editor,
        pendingDeleteId,
        operationError,
    ) { repositoryState, editorState, pendingDelete, listError ->
        val statuses = repositoryState.syncState.sources.associateBy(SourceSyncStatus::sourceKey)
        CustomSourcesUiState(
            isLoading = false,
            sources = repositoryState.sources.map { source ->
                CustomSourceListItem(source, statuses[source.sourceKey])
            },
            editor = editorState,
            pendingDeleteId = pendingDelete,
            operationError = listError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CustomSourcesUiState(),
    )

    fun startAdding() {
        operationError.value = null
        editor.value = CustomSourceEditorState(
            id = UUID.randomUUID().toString(),
            timezoneId = ZoneId.systemDefault().id,
        )
    }

    fun startEditing(source: CustomContestSource) {
        operationError.value = null
        editor.value = CustomSourceEditorState(
            id = source.id,
            name = source.name,
            url = source.url,
            timezoneId = source.timezoneId,
            format = source.format,
            itemSelector = source.selectors.item,
            titleSelector = source.selectors.title,
            startSelector = source.selectors.start,
            endSelector = source.selectors.end,
            linkSelector = source.selectors.link,
            dateTimePattern = source.selectors.dateTimePattern,
            isAdvancedExpanded = source.selectors.hasAnyValue,
        )
    }

    fun closeEditor() {
        editor.value = null
    }

    fun updateName(value: String) = updateEditor { copy(name = value) }
    fun updateUrl(value: String) = updateEditor { copy(url = value) }
    fun updateTimezone(value: String) = updateEditor { copy(timezoneId = value) }
    fun updateItemSelector(value: String) = updateEditor { copy(itemSelector = value) }
    fun updateTitleSelector(value: String) = updateEditor { copy(titleSelector = value) }
    fun updateStartSelector(value: String) = updateEditor { copy(startSelector = value) }
    fun updateEndSelector(value: String) = updateEditor { copy(endSelector = value) }
    fun updateLinkSelector(value: String) = updateEditor { copy(linkSelector = value) }
    fun updateDateTimePattern(value: String) = updateEditor { copy(dateTimePattern = value) }

    fun selectFormat(format: CustomSourceFormat) = updateEditor {
        copy(
            format = format,
            isAdvancedExpanded = isAdvancedExpanded || format == CustomSourceFormat.HTML,
        )
    }

    fun toggleAdvanced() {
        editor.update { state -> state?.copy(isAdvancedExpanded = !state.isAdvancedExpanded) }
    }

    fun preview() {
        val current = editor.value ?: return
        if (current.isPreviewing) return
        editor.update { state -> state?.copy(isPreviewing = true, error = null, preview = null) }
        viewModelScope.launch {
            try {
                val result = previewer.preview(current.toSource(enabled = true))
                editor.update { state ->
                    state?.takeIf { it.hasSameConfigurationAs(current) }?.copy(
                        isPreviewing = false,
                        preview = result.toUi(),
                        error = null,
                    ) ?: state?.copy(isPreviewing = false)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                editor.update { state ->
                    state?.takeIf { it.hasSameConfigurationAs(current) }?.copy(
                        isPreviewing = false,
                        preview = null,
                        error = throwable.toEditorError(),
                    ) ?: state?.copy(isPreviewing = false)
                }
            }
        }
    }

    fun save() {
        val current = editor.value ?: return
        if (current.preview == null || current.isPreviewing) return
        viewModelScope.launch {
            try {
                val existing = uiState.value.sources.firstOrNull { item ->
                    item.source.id == current.id
                }?.source
                customSourceRepository.save(
                    current.toSource(enabled = existing?.enabled ?: true),
                )
                editor.value = null
                contestRepository.refresh()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                editor.update { state -> state?.copy(error = throwable.toEditorError()) }
            }
        }
    }

    fun setEnabled(source: CustomContestSource, enabled: Boolean) {
        viewModelScope.launch {
            try {
                operationError.value = null
                customSourceRepository.setEnabled(source.id, enabled)
                if (enabled) contestRepository.refresh()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                operationError.value = throwable.toEditorError()
            }
        }
    }

    fun requestDelete(id: String) {
        operationError.value = null
        pendingDeleteId.value = id
    }

    fun cancelDelete() {
        pendingDeleteId.value = null
    }

    fun confirmDelete(source: CustomContestSource) {
        viewModelScope.launch {
            try {
                operationError.value = null
                contestRepository.deleteCustomSourceData(source.sourceKey)
                customSourceRepository.delete(source.id)
                pendingDeleteId.value = null
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                operationError.value = throwable.toEditorError()
            }
        }
    }

    private fun updateEditor(transform: CustomSourceEditorState.() -> CustomSourceEditorState) {
        editor.update { current -> current?.transform()?.copy(preview = null, error = null) }
    }
}

private fun CustomSourceEditorState.hasSameConfigurationAs(
    other: CustomSourceEditorState,
): Boolean = id == other.id && name == other.name && url == other.url &&
    timezoneId == other.timezoneId && format == other.format &&
    itemSelector == other.itemSelector && titleSelector == other.titleSelector &&
    startSelector == other.startSelector && endSelector == other.endSelector &&
    linkSelector == other.linkSelector && dateTimePattern == other.dateTimePattern

private fun CustomSourceEditorState.toSource(enabled: Boolean): CustomContestSource {
    val selectors = CustomHtmlSelectors(
        item = itemSelector.trim(),
        title = titleSelector.trim(),
        start = startSelector.trim(),
        end = endSelector.trim(),
        link = linkSelector.trim(),
        dateTimePattern = dateTimePattern.trim(),
    )
    return CustomContestSource(
        id = id,
        name = name.trim(),
        url = url.trim(),
        enabled = enabled,
        format = if (selectors.hasAnyValue) CustomSourceFormat.HTML else format,
        timezoneId = timezoneId.trim(),
        selectors = selectors,
    ).also(CustomContestSource::requireValid)
}

private fun CustomSourceParseResult.toUi(): CustomSourcePreviewUi = CustomSourcePreviewUi(
    detectedFormat = detectedFormat,
    contests = contests,
    warnings = warnings,
)

private fun Throwable.toEditorError(): CustomSourceEditorError = when (this) {
    is RemoteHttpException -> CustomSourceEditorError.HTTP
    is RemoteParsingException -> CustomSourceEditorError.PARSING
    is RemoteApiException -> CustomSourceEditorError.REMOTE
    is IOException -> CustomSourceEditorError.NETWORK
    is IllegalArgumentException -> CustomSourceEditorError.INVALID_INPUT
    else -> CustomSourceEditorError.UNKNOWN
}

private const val STOP_TIMEOUT_MILLIS = 5_000L
