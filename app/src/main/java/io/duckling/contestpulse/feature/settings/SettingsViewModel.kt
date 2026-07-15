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
import io.duckling.contestpulse.domain.update.AppUpdate
import io.duckling.contestpulse.domain.update.AppUpdateCheckResult
import io.duckling.contestpulse.domain.update.AppUpdateError
import io.duckling.contestpulse.domain.update.AppUpdateRepository
import io.duckling.contestpulse.domain.update.AppVersionProvider
import io.duckling.contestpulse.sync.SyncWorkScheduler
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val preferences: SyncPreferences = SyncPreferences(),
    val syncState: ContestSyncState = ContestSyncState(),
    val exactRemindersAvailable: Boolean = true,
    val customSourceCount: Int = 0,
    val appUpdate: AppUpdateUiState = AppUpdateUiState(),
)

data class AppUpdateUiState(
    val currentVersion: String = "",
    val phase: AppUpdatePhase = AppUpdatePhase.Idle,
)

sealed interface AppUpdatePhase {
    data object Idle : AppUpdatePhase

    data object Checking : AppUpdatePhase

    data object UpToDate : AppUpdatePhase

    data class Available(
        val update: AppUpdate,
    ) : AppUpdatePhase

    data class Downloading(
        val progressPercent: Int?,
    ) : AppUpdatePhase

    data class ReadyToInstall(
        val apkFile: File,
    ) : AppUpdatePhase

    data class Error(
        val error: AppUpdateError,
    ) : AppUpdatePhase
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    contestRepository: ContestRepository,
    reminderManager: ReminderManager,
    customSourceRepository: CustomSourceRepository,
    private val syncWorkScheduler: SyncWorkScheduler,
    private val appVersionProvider: AppVersionProvider,
    private val appUpdateRepository: AppUpdateRepository,
) : ViewModel() {
    private val currentAppVersion = appVersionProvider.currentVersion()
    private val appUpdate = MutableStateFlow(
        AppUpdateUiState(currentVersion = currentAppVersion.name),
    )
    private val installEventChannel = Channel<File>(Channel.BUFFERED)
    val installEvents = installEventChannel.receiveAsFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.preferences,
        contestRepository.observeSyncState(),
        customSourceRepository.sources,
        appUpdate,
    ) { preferences, syncState, customSources, update ->
        SettingsUiState(
            isLoading = false,
            preferences = preferences,
            syncState = syncState,
            exactRemindersAvailable = reminderManager.canScheduleExactReminders(),
            customSourceCount = customSources.size,
            appUpdate = update,
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

    fun setDefaultReminderOffsetMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setDefaultReminderOffsetMinutes(minutes)
        }
    }

    fun setSourceEnabled(
        source: ContestSource,
        enabled: Boolean,
    ) = updatePreferences {
        settingsRepository.setSourceEnabled(source, enabled)
    }

    fun checkForAppUpdate() {
        if (appUpdate.value.phase.isBusy()) return
        viewModelScope.launch {
            appUpdate.value = appUpdate.value.copy(phase = AppUpdatePhase.Checking)
            appUpdate.value = when (val result = appUpdateRepository.checkForUpdate(currentAppVersion)) {
                AppUpdateCheckResult.UpToDate -> appUpdate.value.copy(
                    phase = AppUpdatePhase.UpToDate,
                )

                is AppUpdateCheckResult.UpdateAvailable -> appUpdate.value.copy(
                    phase = AppUpdatePhase.Available(result.update),
                )

                is AppUpdateCheckResult.Failure -> appUpdate.value.copy(
                    phase = AppUpdatePhase.Error(result.error),
                )
            }
        }
    }

    fun downloadAppUpdate() {
        val update = (appUpdate.value.phase as? AppUpdatePhase.Available)?.update ?: return
        viewModelScope.launch {
            appUpdate.value = appUpdate.value.copy(
                phase = AppUpdatePhase.Downloading(progressPercent = null),
            )
            when (val result = appUpdateRepository.downloadUpdate(update) { downloaded, total ->
                appUpdate.value = appUpdate.value.copy(
                    phase = AppUpdatePhase.Downloading(
                        progressPercent = total.takeIf { it > 0L }
                            ?.let { size -> ((downloaded * 100L) / size).toInt().coerceIn(0, 100) },
                    ),
                )
            }) {
                is io.duckling.contestpulse.domain.update.AppUpdateDownloadResult.Success -> {
                    appUpdate.value = appUpdate.value.copy(
                        phase = AppUpdatePhase.ReadyToInstall(result.apkFile),
                    )
                    installEventChannel.send(result.apkFile)
                }

                is io.duckling.contestpulse.domain.update.AppUpdateDownloadResult.Failure -> {
                    appUpdate.value = appUpdate.value.copy(
                        phase = AppUpdatePhase.Error(result.error),
                    )
                }
            }
        }
    }

    fun reportInstallLaunchFailure() {
        appUpdate.value = appUpdate.value.copy(phase = AppUpdatePhase.Error(AppUpdateError.UNKNOWN))
    }

    fun installDownloadedAppUpdate() {
        val apkFile = (appUpdate.value.phase as? AppUpdatePhase.ReadyToInstall)?.apkFile ?: return
        viewModelScope.launch { installEventChannel.send(apkFile) }
    }

    private fun updatePreferences(update: suspend () -> Unit) {
        viewModelScope.launch {
            update()
            syncWorkScheduler.applyPreferences()
        }
    }

    override fun onCleared() {
        installEventChannel.close()
        super.onCleared()
    }
}

private fun AppUpdatePhase.isBusy(): Boolean =
    this is AppUpdatePhase.Checking || this is AppUpdatePhase.Downloading

private const val STOP_TIMEOUT_MILLIS = 5_000L
