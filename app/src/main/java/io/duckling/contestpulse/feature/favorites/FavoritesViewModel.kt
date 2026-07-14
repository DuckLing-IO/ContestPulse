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
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    repository: ContestRepository,
    minuteTicker: MinuteTicker,
    private val contestRepository: ContestRepository,
) : ViewModel() {
    private val selectedSegment = MutableStateFlow(FavoriteSegment.UPCOMING)

    val uiState: StateFlow<FavoritesUiState> = combine(
        repository.observeContests(),
        minuteTicker.stream(),
        selectedSegment,
    ) { contests, now, segment ->
        val favorites = contests
            .filter(Contest::isFavorite)
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
}

private const val STOP_TIMEOUT_MILLIS = 5_000L
