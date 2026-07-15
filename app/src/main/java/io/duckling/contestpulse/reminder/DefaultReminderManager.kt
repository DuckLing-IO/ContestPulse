package io.duckling.contestpulse.reminder

import androidx.room.withTransaction
import io.duckling.contestpulse.core.database.ContestPulseDatabase
import io.duckling.contestpulse.core.database.dao.ContestDao
import io.duckling.contestpulse.core.database.dao.FavoriteDao
import io.duckling.contestpulse.core.database.dao.ReminderDao
import io.duckling.contestpulse.core.database.entity.FavoriteEntity
import io.duckling.contestpulse.core.database.entity.ReminderEntity
import io.duckling.contestpulse.domain.logic.calculateReminderTrigger
import io.duckling.contestpulse.domain.logic.stableReminderRequestCode
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.reminder.ReminderManager
import io.duckling.contestpulse.domain.reminder.ReminderToggleResult
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultReminderManager @Inject constructor(
    private val database: ContestPulseDatabase,
    private val contestDao: ContestDao,
    private val favoriteDao: FavoriteDao,
    private val reminderDao: ReminderDao,
    private val scheduler: ReminderScheduler,
    private val clock: Clock,
) : ReminderManager {
    override fun canScheduleExactReminders(): Boolean = scheduler.canScheduleExact()

    override suspend fun toggleReminder(
        contestId: String,
        offset: Duration,
    ): ReminderToggleResult {
        require(!offset.isNegative) { "Reminder offset must not be negative" }
        require(offset.seconds % SECONDS_PER_MINUTE == 0L) {
            "Reminder offset must use whole minutes"
        }
        val offsetMinutes = offset.toMinutes()
        val existing = reminderDao.getByContestAndOffset(contestId, offsetMinutes)
        if (existing != null) {
            scheduler.cancel(existing)
            reminderDao.delete(existing.id)
            return ReminderToggleResult.Removed
        }

        val contest = checkNotNull(contestDao.getById(contestId)) {
            "Contest not found: $contestId"
        }
        val triggerAt = calculateReminderTrigger(
            contestStart = Instant.ofEpochMilli(contest.startTimeEpochMillis),
            offset = offset,
            now = clock.instant(),
        ) ?: return ReminderToggleResult.TooLate
        val reminderId = "$contestId:$offsetMinutes"
        val reminder = ReminderEntity(
            id = reminderId,
            contestId = contestId,
            triggerAtEpochMillis = triggerAt.toEpochMilli(),
            offsetMinutes = offsetMinutes,
            isEnabled = true,
            schedulerType = scheduler.schedulerType(),
            systemRequestCode = stableReminderRequestCode(reminderId),
            createdAtEpochMillis = clock.millis(),
        )

        database.withTransaction {
            if (!favoriteDao.isFavorite(contestId)) {
                favoriteDao.insert(
                    FavoriteEntity(
                        contestId = contestId,
                        createdAtEpochMillis = clock.millis(),
                        note = null,
                    ),
                )
            }
            reminderDao.upsert(reminder)
        }
        try {
            scheduler.schedule(reminder)
        } catch (throwable: Throwable) {
            reminderDao.delete(reminder.id)
            throw throwable
        }
        return ReminderToggleResult.Scheduled(
            isExact = reminder.schedulerType == AndroidAlarmReminderScheduler.SCHEDULER_EXACT,
        )
    }

    override suspend fun scheduleDefaultReminder(
        contestId: String,
        offset: Duration,
    ) {
        require(!offset.isNegative) { "Reminder offset must not be negative" }
        require(offset.seconds % SECONDS_PER_MINUTE == 0L) {
            "Reminder offset must use whole minutes"
        }
        val offsetMinutes = offset.toMinutes()
        if (reminderDao.getByContestAndOffset(contestId, offsetMinutes) != null) return

        val contest = checkNotNull(contestDao.getById(contestId)) {
            "Contest not found: $contestId"
        }
        val triggerAt = calculateReminderTrigger(
            contestStart = Instant.ofEpochMilli(contest.startTimeEpochMillis),
            offset = offset,
            now = clock.instant(),
        ) ?: return
        val reminderId = "$contestId:$offsetMinutes"
        val reminder = ReminderEntity(
            id = reminderId,
            contestId = contestId,
            triggerAtEpochMillis = triggerAt.toEpochMilli(),
            offsetMinutes = offsetMinutes,
            isEnabled = true,
            schedulerType = scheduler.schedulerType(),
            systemRequestCode = stableReminderRequestCode(reminderId),
            createdAtEpochMillis = clock.millis(),
        )
        reminderDao.upsert(reminder)
        try {
            scheduler.schedule(reminder)
        } catch (throwable: Throwable) {
            reminderDao.delete(reminder.id)
            throw throwable
        }
    }

    override suspend fun clearForContest(contestId: String) {
        val reminders = reminderDao.getEnabledForContest(contestId)
        reminders.forEach(scheduler::cancel)
        reminderDao.deleteForContest(contestId)
    }

    override suspend fun rescheduleForContests(contests: List<Contest>) {
        contests.forEach { contest ->
            reminderDao.getEnabledForContest(contest.id).forEach { reminder ->
                reschedule(reminder, contest.startTime)
            }
        }
    }

    override suspend fun rescheduleAll() {
        reminderDao.getAllEnabled().forEach { reminder ->
            val contest = contestDao.getById(reminder.contestId)
            if (contest == null) {
                scheduler.cancel(reminder)
                reminderDao.delete(reminder.id)
            } else {
                reschedule(
                    reminder = reminder,
                    contestStart = Instant.ofEpochMilli(contest.startTimeEpochMillis),
                )
            }
        }
    }

    private suspend fun reschedule(
        reminder: ReminderEntity,
        contestStart: Instant,
    ) {
        val triggerAt = calculateReminderTrigger(
            contestStart = contestStart,
            offset = Duration.ofMinutes(reminder.offsetMinutes),
            now = clock.instant(),
        )
        scheduler.cancel(reminder)
        if (triggerAt == null) {
            reminderDao.delete(reminder.id)
            return
        }
        val updated = reminder.copy(
            triggerAtEpochMillis = triggerAt.toEpochMilli(),
            schedulerType = scheduler.schedulerType(),
        )
        reminderDao.upsert(updated)
        runCatching { scheduler.schedule(updated) }
    }
}

private const val SECONDS_PER_MINUTE = 60L
