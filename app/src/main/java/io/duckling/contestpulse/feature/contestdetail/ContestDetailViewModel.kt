package io.duckling.contestpulse.feature.contestdetail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.duckling.contestpulse.core.time.MinuteTicker
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.repository.ContestRepository
import io.duckling.contestpulse.domain.reminder.ReminderManager
import io.duckling.contestpulse.domain.reminder.ReminderToggleResult
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

data class ContestDetailUiState(
    val isLoading: Boolean = true,
    val contest: Contest? = null,
    val now: Instant = Instant.EPOCH,
    val canScheduleExactReminders: Boolean = true,
    val reminderMessage: ReminderUiMessage? = null,
)

enum class ReminderUiMessage {
    SCHEDULED_EXACT,
    SCHEDULED_INEXACT,
    REMOVED,
    TOO_LATE,
    NOTIFICATION_PERMISSION_DENIED,
    FAILED,
}

@HiltViewModel
class ContestDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: ContestRepository,
    minuteTicker: MinuteTicker,
    private val contestRepository: ContestRepository,
    private val reminderManager: ReminderManager,
) : ViewModel() {
    private val contestId = Uri.decode(
        checkNotNull(savedStateHandle[CONTEST_ID_ARGUMENT]),
    )

    private val reminderMessage = MutableStateFlow<ReminderUiMessage?>(null)

    val uiState: StateFlow<ContestDetailUiState> = combine(
        repository.observeContest(contestId),
        minuteTicker.stream(),
        reminderMessage,
    ) { contest, now, message ->
        ContestDetailUiState(
            isLoading = false,
            contest = contest,
            now = now,
            canScheduleExactReminders = reminderManager.canScheduleExactReminders(),
            reminderMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ContestDetailUiState(),
    )

    fun toggleFavorite() {
        viewModelScope.launch {
            contestRepository.toggleFavorite(contestId)
        }
    }

    fun toggleReminder(offset: Duration) {
        viewModelScope.launch {
            reminderMessage.value = try {
                when (val result = reminderManager.toggleReminder(contestId, offset)) {
                    is ReminderToggleResult.Scheduled -> if (result.isExact) {
                        ReminderUiMessage.SCHEDULED_EXACT
                    } else {
                        ReminderUiMessage.SCHEDULED_INEXACT
                    }
                    ReminderToggleResult.Removed -> ReminderUiMessage.REMOVED
                    ReminderToggleResult.TooLate -> ReminderUiMessage.TOO_LATE
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                ReminderUiMessage.FAILED
            }
        }
    }

    fun notificationPermissionDenied() {
        reminderMessage.value = ReminderUiMessage.NOTIFICATION_PERMISSION_DENIED
    }

    companion object {
        const val CONTEST_ID_ARGUMENT = "contestId"
    }
}

private const val STOP_TIMEOUT_MILLIS = 5_000L
