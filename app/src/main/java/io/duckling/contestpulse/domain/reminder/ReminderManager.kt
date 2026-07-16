package io.duckling.contestpulse.domain.reminder

import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ReminderDefinition
import io.duckling.contestpulse.domain.model.ReminderRule
import java.time.Duration

sealed interface ReminderToggleResult {
    data class Scheduled(val isExact: Boolean) : ReminderToggleResult
    data object Removed : ReminderToggleResult
    data object TooLate : ReminderToggleResult
    data object Invalid : ReminderToggleResult
    data object Duplicate : ReminderToggleResult
}

interface ReminderManager {
    fun canScheduleExactReminders(): Boolean

    suspend fun toggleReminder(
        contestId: String,
        offset: Duration,
    ): ReminderToggleResult

    suspend fun scheduleDefaultReminder(
        contestId: String,
        offset: Duration,
    )

    suspend fun clearForContest(contestId: String)

    suspend fun addReminder(contestId: String, rule: ReminderRule): ReminderToggleResult

    suspend fun updateReminder(reminderId: String, rule: ReminderRule): ReminderToggleResult

    suspend fun deleteReminder(reminderId: String): ReminderToggleResult

    suspend fun favoriteWithDefaultReminders(
        contestId: String,
        reminders: List<ReminderDefinition>,
    )

    suspend fun removeFavoriteAndReminders(contestId: String)

    suspend fun replaceCustomReminders(
        contestId: String,
        reminders: List<ReminderDefinition>,
    )

    fun requestReconcile()

    suspend fun reconcileNow()

    suspend fun rescheduleForContests(contests: List<Contest>)

    suspend fun rescheduleAll()
}
