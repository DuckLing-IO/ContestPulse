package io.duckling.contestpulse.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.duckling.contestpulse.domain.repository.ContestRepository

@HiltWorker
class ContestSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val contestRepository: ContestRepository,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val report = contestRepository.refresh()
        if (report.alreadyRunning || report.results.isEmpty()) return Result.success()
        return if (report.results.any { result -> result.issue == null }) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
