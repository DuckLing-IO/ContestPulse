package io.duckling.contestpulse.domain.reminder

import io.duckling.contestpulse.domain.model.Contest
import java.time.Duration

sealed interface ReminderToggleResult {
    data class Scheduled(val isExact: Boolean) : ReminderToggleResult
    data object Removed : ReminderToggleResult
    data object TooLate : ReminderToggleResult
}

interface ReminderManager {
    fun canScheduleExactReminders(): Boolean

    suspend fun toggleReminder(
        contestId: String,
        offset: Duration,
    ): ReminderToggleResult

    suspend fun clearForContest(contestId: String)

    suspend fun rescheduleForContests(contests: List<Contest>)

    suspend fun rescheduleAll()
}
