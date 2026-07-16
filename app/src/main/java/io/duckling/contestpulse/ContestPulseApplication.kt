package io.duckling.contestpulse

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import io.duckling.contestpulse.reminder.ReminderNotifier
import io.duckling.contestpulse.domain.reminder.ReminderManager
import io.duckling.contestpulse.sync.SyncWorkScheduler
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class ContestPulseApplication : Application(), Configuration.Provider {
    @Inject lateinit var reminderNotifier: ReminderNotifier
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncWorkScheduler: SyncWorkScheduler
    @Inject lateinit var reminderManager: ReminderManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        reminderNotifier.ensureChannel()
        reminderManager.requestReconcile()
        applicationScope.launch { syncWorkScheduler.applyPreferences() }
    }
}

private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
