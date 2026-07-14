package io.duckling.contestpulse.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.duckling.contestpulse.domain.settings.SettingsRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class WorkManagerSyncWorkScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val settingsRepository: SettingsRepository,
) : SyncWorkScheduler {
    override suspend fun applyPreferences() {
        val preferences = settingsRepository.preferences.first()
        if (!preferences.backgroundSyncEnabled || preferences.enabledSources.isEmpty()) {
            workManager.cancelUniqueWork(UNIQUE_SYNC_WORK)
            return
        }
        val networkType = if (preferences.wifiOnly) {
            NetworkType.UNMETERED
        } else {
            NetworkType.CONNECTED
        }
        val request = PeriodicWorkRequestBuilder<ContestSyncWorker>(
            preferences.intervalHours.toLong(),
            TimeUnit.HOURS,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(networkType)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_MINUTES,
                TimeUnit.MINUTES,
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_SYNC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}

private const val UNIQUE_SYNC_WORK = "contest-schedule-sync"
private const val BACKOFF_MINUTES = 30L
