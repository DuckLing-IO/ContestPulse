package io.duckling.contestpulse.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.customsource.CustomSourceRepository
import io.duckling.contestpulse.domain.model.ContestSyncState
import io.duckling.contestpulse.domain.reminder.ReminderManager
import io.duckling.contestpulse.domain.repository.ContestRepository
import io.duckling.contestpulse.domain.settings.SettingsRepository
import io.duckling.contestpulse.domain.settings.SyncPreferences
import io.duckling.contestpulse.sync.SyncWorkScheduler
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val preferences: SyncPreferences = SyncPreferences(),
    val syncState: ContestSyncState = ContestSyncState(),
    val exactRemindersAvailable: Boolean = true,
    val customSourceCount: Int = 0,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    contestRepository: ContestRepository,
    reminderManager: ReminderManager,
    customSourceRepository: CustomSourceRepository,
    private val syncWorkScheduler: SyncWorkScheduler,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.preferences,
        contestRepository.observeSyncState(),
        customSourceRepository.sources,
    ) { preferences, syncState, customSources ->
        SettingsUiState(
            isLoading = false,
            preferences = preferences,
            syncState = syncState,
            exactRemindersAvailable = reminderManager.canScheduleExactReminders(),
            customSourceCount = customSources.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState(),
    )

    fun setBackgroundSyncEnabled(enabled: Boolean) = updatePreferences {
        settingsRepository.setBackgroundSyncEnabled(enabled)
    }

    fun setWifiOnly(enabled: Boolean) = updatePreferences {
        settingsRepository.setWifiOnly(enabled)
    }

    fun setIntervalHours(hours: Int) = updatePreferences {
        settingsRepository.setIntervalHours(hours)
    }

    fun setSourceEnabled(
        source: ContestSource,
        enabled: Boolean,
    ) = updatePreferences {
        settingsRepository.setSourceEnabled(source, enabled)
    }

    private fun updatePreferences(update: suspend () -> Unit) {
        viewModelScope.launch {
            update()
            syncWorkScheduler.applyPreferences()
        }
    }
}

private const val STOP_TIMEOUT_MILLIS = 5_000L
