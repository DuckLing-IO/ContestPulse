package io.duckling.contestpulse.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.duckling.contestpulse.core.time.MinuteTicker
import io.duckling.contestpulse.domain.logic.statusAt
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestStatus
import io.duckling.contestpulse.domain.repository.ContestRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import io.duckling.contestpulse.feature.common.ContestDisplayMode
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FavoriteSegment {
    UPCOMING,
    FINISHED,
    ALL,
}

data class FavoritesUiState(
    val isLoading: Boolean = true,
    val selectedSegment: FavoriteSegment = FavoriteSegment.UPCOMING,
    val contests: List<Contest> = emptyList(),
    val now: Instant = Instant.EPOCH,
    val displayMode: ContestDisplayMode = ContestDisplayMode.LIST,
    val calendarMonth: YearMonth = YearMonth.now(),
    val selectedCalendarDate: LocalDate? = null,
    val calendarContests: List<Contest> = emptyList(),
)

private data class FavoriteCalendarUiState(
    val displayMode: ContestDisplayMode,
    val month: YearMonth,
    val selectedDate: LocalDate?,
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    repository: ContestRepository,
    minuteTicker: MinuteTicker,
    private val contestRepository: ContestRepository,
    zoneId: ZoneId,
) : ViewModel() {
    private val selectedSegment = MutableStateFlow(FavoriteSegment.UPCOMING)
    private val displayMode = MutableStateFlow(ContestDisplayMode.LIST)
    private val calendarMonth = MutableStateFlow(YearMonth.now(zoneId))
    private val selectedCalendarDate = MutableStateFlow<LocalDate?>(null)
    private val calendarUiState = combine(
        displayMode,
        calendarMonth,
        selectedCalendarDate,
    ) { currentDisplayMode, month, selectedDate ->
        FavoriteCalendarUiState(
            displayMode = currentDisplayMode,
            month = month,
            selectedDate = selectedDate,
        )
    }

    val uiState: StateFlow<FavoritesUiState> = combine(
        repository.observeContests(),
        minuteTicker.stream(),
        selectedSegment,
        calendarUiState,
    ) { contests, now, segment, calendarState ->
        val allFavorites = contests
            .filter(Contest::isFavorite)
            .sortedBy(Contest::startTime)
        val favorites = allFavorites
            .filter { contest ->
                when (segment) {
                    FavoriteSegment.UPCOMING -> contest.statusAt(now) != ContestStatus.FINISHED
                    FavoriteSegment.FINISHED -> contest.statusAt(now) == ContestStatus.FINISHED
                    FavoriteSegment.ALL -> true
                }
            }
            .sortedBy(Contest::startTime)

        FavoritesUiState(
            isLoading = false,
            selectedSegment = segment,
            contests = favorites,
            now = now,
            displayMode = calendarState.displayMode,
            calendarMonth = calendarState.month,
            selectedCalendarDate = calendarState.selectedDate,
            calendarContests = allFavorites,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = FavoritesUiState(),
    )

    fun selectSegment(segment: FavoriteSegment) {
        selectedSegment.update { segment }
    }

    fun toggleFavorite(contestId: String) {
        viewModelScope.launch {
            contestRepository.toggleFavorite(contestId)
        }
    }

    fun toggleDisplayMode() {
        selectedCalendarDate.value = null
        displayMode.update { mode ->
            if (mode == ContestDisplayMode.LIST) ContestDisplayMode.CALENDAR else ContestDisplayMode.LIST
        }
    }

    fun selectCalendarDate(date: LocalDate) {
        selectedCalendarDate.value = date
    }

    fun returnToCalendar() {
        selectedCalendarDate.value = null
    }

    fun showPreviousCalendarMonth() {
        calendarMonth.update { it.minusMonths(1) }
    }

    fun showNextCalendarMonth() {
        calendarMonth.update { it.plusMonths(1) }
    }
}

private const val STOP_TIMEOUT_MILLIS = 5_000L
