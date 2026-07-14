package io.duckling.contestpulse.feature.contestlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.duckling.contestpulse.core.time.MinuteTicker
import io.duckling.contestpulse.domain.logic.buildContestTimeline
import io.duckling.contestpulse.domain.logic.filterContests
import io.duckling.contestpulse.domain.customsource.CustomSourceRepository
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestDateRange
import io.duckling.contestpulse.domain.model.ContestFilter
import io.duckling.contestpulse.domain.model.ContestGroup
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.model.SyncErrorType
import io.duckling.contestpulse.domain.model.SourceSyncStatus
import io.duckling.contestpulse.domain.repository.ContestRepository
import io.duckling.contestpulse.domain.settings.SettingsRepository
import java.time.Instant
import java.time.Duration
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContestListUiState(
    val isLoading: Boolean = true,
    val groups: List<ContestGroup> = emptyList(),
    val nextContest: Contest? = null,
    val now: Instant = Instant.EPOCH,
    val lastUpdatedAt: Instant? = null,
    val isRefreshing: Boolean = false,
    val syncIssueType: SyncErrorType? = null,
    val failedSources: List<FailedSyncSource> = emptyList(),
    val hasSuccessfulSource: Boolean = false,
    val hasCompletedSync: Boolean = false,
    val filter: ContestFilter = ContestFilter(),
    val isFilterExpanded: Boolean = false,
    val activeFilterCount: Int = 0,
    val filteredContestCount: Int = 0,
)

data class FailedSyncSource(
    val source: ContestSource,
    val displayName: String? = null,
)

private data class NamedSyncState(
    val isRefreshing: Boolean,
    val sources: List<SourceSyncStatus>,
    val customSourceNames: Map<String, String>,
)

@HiltViewModel
class ContestListViewModel @Inject constructor(
    minuteTicker: MinuteTicker,
    private val contestRepository: ContestRepository,
    customSourceRepository: CustomSourceRepository,
    settingsRepository: SettingsRepository,
    zoneId: ZoneId,
) : ViewModel() {
    private val filter = MutableStateFlow(ContestFilter())
    private val isFilterExpanded = MutableStateFlow(false)

    private val namedSyncState = combine(
        contestRepository.observeSyncState(),
        customSourceRepository.sources,
        settingsRepository.preferences,
    ) { syncState, customSources, preferences ->
        val enabledSourceKeys = preferences.enabledSources.mapTo(mutableSetOf(), ContestSource::name)
        enabledSourceKeys += customSources.filter { source -> source.enabled }
            .map { source -> source.sourceKey }
        NamedSyncState(
            isRefreshing = syncState.isRefreshing,
            sources = syncState.sources.filter { status -> status.sourceKey in enabledSourceKeys },
            customSourceNames = customSources.associate { source -> source.sourceKey to source.name },
        )
    }

    val uiState: StateFlow<ContestListUiState> = combine(
        contestRepository.observeContests(),
        minuteTicker.stream(),
        namedSyncState,
        filter,
        isFilterExpanded,
    ) { contests, now, syncState, currentFilter, filterExpanded ->
        val filteredContests = filterContests(contests, currentFilter, now)
        val timeline = buildContestTimeline(filteredContests, now, zoneId)
        val nextContest = timeline
            .flatMap(ContestGroup::contests)
            .firstOrNull { !it.startTime.isBefore(now) }
        val groups = timeline
            .map { group ->
                group.copy(
                    contests = group.contests.filterNot { it.id == nextContest?.id },
                )
            }
            .filter { it.contests.isNotEmpty() }
        ContestListUiState(
            isLoading = contests.isEmpty() &&
                (syncState.isRefreshing || syncState.sources.isEmpty()),
            groups = groups,
            nextContest = nextContest,
            now = now,
            lastUpdatedAt = syncState.sources
                .mapNotNull { status -> status.lastSuccessAt }
                .maxOrNull()
                ?: contests.maxOfOrNull(Contest::lastUpdatedAt),
            isRefreshing = syncState.isRefreshing,
            syncIssueType = syncState.sources.firstNotNullOfOrNull { status ->
                status.issue?.type
            },
            failedSources = syncState.sources.mapNotNull { status ->
                status.issue?.let {
                    FailedSyncSource(
                        source = status.source,
                        displayName = syncState.customSourceNames[status.sourceKey],
                    )
                }
            },
            hasSuccessfulSource = syncState.sources.any { status -> status.issue == null },
            hasCompletedSync = syncState.sources.isNotEmpty(),
            filter = currentFilter,
            isFilterExpanded = filterExpanded,
            activeFilterCount = currentFilter.activeFilterCount(),
            filteredContestCount = timeline.sumOf { it.contests.size },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ContestListUiState(),
    )

    init {
        viewModelScope.launch {
            contestRepository.refreshIfStale(INITIAL_REFRESH_MAX_AGE)
        }
    }

    fun toggleFavorite(contestId: String) {
        viewModelScope.launch {
            contestRepository.toggleFavorite(contestId)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            contestRepository.refresh()
        }
    }

    fun toggleFilterExpanded() {
        isFilterExpanded.update(Boolean::not)
    }

    fun toggleSource(source: ContestSource) {
        filter.update { current ->
            current.copy(
                sources = if (source in current.sources) {
                    current.sources - source
                } else {
                    current.sources + source
                },
            )
        }
    }

    fun selectDateRange(dateRange: ContestDateRange) {
        filter.update { it.copy(dateRange = dateRange) }
    }

    fun toggleRatedOnly() {
        filter.update { it.copy(ratedOnly = !it.ratedOnly) }
    }

    fun toggleFavoriteOnly() {
        filter.update { it.copy(favoriteOnly = !it.favoriteOnly) }
    }

    fun clearFilters() {
        filter.value = ContestFilter()
        isFilterExpanded.value = false
    }
}

private fun ContestFilter.activeFilterCount(): Int =
    listOf(
        FILTERABLE_SOURCES.any { it !in sources },
        dateRange != ContestDateRange.ALL,
        ratedOnly,
        favoriteOnly,
    ).count { it }

private val FILTERABLE_SOURCES = setOf(
    ContestSource.CODEFORCES,
    ContestSource.ATCODER,
    ContestSource.LUOGU,
    ContestSource.NOWCODER,
    ContestSource.OTHER,
)

private const val STOP_TIMEOUT_MILLIS = 5_000L
private val INITIAL_REFRESH_MAX_AGE: Duration = Duration.ofHours(1)
