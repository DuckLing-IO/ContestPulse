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
import io.duckling.contestpulse.domain.model.ReminderDefinition
import io.duckling.contestpulse.domain.model.ReminderRule
import io.duckling.contestpulse.domain.settings.SettingsRepository
import io.duckling.contestpulse.core.time.TimeZoneProvider
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
import java.time.ZoneId
import java.util.UUID

data class ContestDetailUiState(
    val isLoading: Boolean = true,
    val contest: Contest? = null,
    val now: Instant = Instant.EPOCH,
    val canScheduleExactReminders: Boolean = true,
    val reminderMessage: ReminderUiMessage? = null,
    val defaultReminders: List<ReminderDefinition> = emptyList(),
    val zoneId: ZoneId = ZoneId.of("UTC"),
)

enum class ReminderUiMessage {
    SCHEDULED_EXACT,
    SCHEDULED_INEXACT,
    REMOVED,
    TOO_LATE,
    NOTIFICATION_PERMISSION_DENIED,
    FAILED,
    DUPLICATE,
    INVALID,
}

@HiltViewModel
class ContestDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: ContestRepository,
    minuteTicker: MinuteTicker,
    private val contestRepository: ContestRepository,
    private val reminderManager: ReminderManager,
    settingsRepository: SettingsRepository,
    private val timeZoneProvider: TimeZoneProvider,
) : ViewModel() {
    private val contestId = Uri.decode(
        checkNotNull(savedStateHandle[CONTEST_ID_ARGUMENT]),
    )

    private val reminderMessage = MutableStateFlow<ReminderUiMessage?>(null)

    val uiState: StateFlow<ContestDetailUiState> = combine(
        repository.observeContest(contestId),
        minuteTicker.stream(),
        reminderMessage,
        settingsRepository.preferences,
    ) { contest, now, message, preferences ->
        ContestDetailUiState(
            isLoading = false,
            contest = contest,
            now = now,
            canScheduleExactReminders = reminderManager.canScheduleExactReminders(),
            reminderMessage = message,
            defaultReminders = preferences.defaultReminders,
            zoneId = timeZoneProvider.currentZoneId(),
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
                    ReminderToggleResult.Invalid -> ReminderUiMessage.FAILED
                    ReminderToggleResult.Duplicate -> ReminderUiMessage.DUPLICATE
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                ReminderUiMessage.FAILED
            }
        }
    }

    fun saveReminder(reminderId: String?, rule: ReminderRule) {
        viewModelScope.launch {
            val state = uiState.value
            val contest = state.contest ?: return@launch
            val storedReminder = reminderId?.let { id -> contest.reminders.firstOrNull { it.id == id } }
            val result = try {
                when {
                    storedReminder != null -> reminderManager.updateReminder(storedReminder.id, rule)
                    reminderId != null -> {
                        val definitions = state.defaultReminders.map { definition ->
                            if (definition.id == reminderId) definition.copy(rule = rule) else definition
                        }
                        reminderManager.replaceCustomReminders(contest.id, definitions)
                        ReminderToggleResult.Scheduled(reminderManager.canScheduleExactReminders())
                    }
                    !contest.isFavorite && contest.reminders.isEmpty() -> {
                        val definitions = state.defaultReminders + ReminderDefinition(
                            id = UUID.randomUUID().toString(),
                            rule = rule,
                            createdAt = state.now,
                        )
                        reminderManager.replaceCustomReminders(contest.id, definitions)
                        ReminderToggleResult.Scheduled(reminderManager.canScheduleExactReminders())
                    }
                    else -> reminderManager.addReminder(contest.id, rule)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                ReminderToggleResult.Invalid
            }
            reminderMessage.value = result.toUiMessage()
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            val state = uiState.value
            val contest = state.contest ?: return@launch
            val stored = contest.reminders.any { it.id == reminderId }
            reminderMessage.value = try {
                if (stored) {
                    reminderManager.deleteReminder(reminderId).toUiMessage()
                } else {
                    reminderManager.replaceCustomReminders(
                        contest.id,
                        state.defaultReminders.filterNot { it.id == reminderId },
                    )
                    ReminderUiMessage.REMOVED
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

private fun ReminderToggleResult.toUiMessage(): ReminderUiMessage = when (this) {
    is ReminderToggleResult.Scheduled -> if (isExact) {
        ReminderUiMessage.SCHEDULED_EXACT
    } else {
        ReminderUiMessage.SCHEDULED_INEXACT
    }
    ReminderToggleResult.Removed -> ReminderUiMessage.REMOVED
    ReminderToggleResult.TooLate -> ReminderUiMessage.TOO_LATE
    ReminderToggleResult.Invalid -> ReminderUiMessage.INVALID
    ReminderToggleResult.Duplicate -> ReminderUiMessage.DUPLICATE
}

private const val STOP_TIMEOUT_MILLIS = 5_000L
