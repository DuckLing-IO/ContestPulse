package io.duckling.contestpulse.reminder

import androidx.room.withTransaction
import io.duckling.contestpulse.core.database.ContestPulseDatabase
import io.duckling.contestpulse.core.database.dao.ContestDao
import io.duckling.contestpulse.core.database.dao.FavoriteDao
import io.duckling.contestpulse.core.database.dao.PendingAlarmCleanupDao
import io.duckling.contestpulse.core.database.dao.ReminderDao
import io.duckling.contestpulse.core.database.entity.FavoriteEntity
import io.duckling.contestpulse.core.database.entity.PendingAlarmCleanupEntity
import io.duckling.contestpulse.core.database.entity.ReminderEntity
import io.duckling.contestpulse.core.time.TimeZoneProvider
import io.duckling.contestpulse.domain.logic.DerivedReminderTrigger
import io.duckling.contestpulse.domain.logic.ReminderValidationError
import io.duckling.contestpulse.domain.logic.deriveReminderTrigger
import io.duckling.contestpulse.domain.logic.hasDuplicateReminder
import io.duckling.contestpulse.domain.logic.stableReminderRequestCode
import io.duckling.contestpulse.domain.logic.validateReminderStructure
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ReminderDefinition
import io.duckling.contestpulse.domain.model.ReminderDeliveryStatus
import io.duckling.contestpulse.domain.model.ReminderFailureReason
import io.duckling.contestpulse.domain.model.ReminderMode
import io.duckling.contestpulse.domain.model.ReminderRule
import io.duckling.contestpulse.domain.model.ReminderScheduleStatus
import io.duckling.contestpulse.domain.model.ruleKey
import io.duckling.contestpulse.domain.reminder.ReminderManager
import io.duckling.contestpulse.domain.reminder.ReminderToggleResult
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class DefaultReminderManager @Inject constructor(
    private val database: ContestPulseDatabase,
    private val contestDao: ContestDao,
    private val favoriteDao: FavoriteDao,
    private val reminderDao: ReminderDao,
    private val cleanupDao: PendingAlarmCleanupDao,
    private val scheduler: ReminderScheduler,
    private val clock: Clock,
    private val timeZoneProvider: TimeZoneProvider,
) : ReminderManager {
    private val reconcileMutex = Mutex()
    private val reconcileRequests = Channel<Unit>(Channel.CONFLATED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            for (ignored in reconcileRequests) reconcileNow()
        }
    }

    override fun canScheduleExactReminders(): Boolean = scheduler.canScheduleExact()

    override suspend fun toggleReminder(
        contestId: String,
        offset: Duration,
    ): ReminderToggleResult {
        require(!offset.isNegative && offset.seconds % 60L == 0L)
        val rule = ReminderRule.Relative(offset.toMinutes().toInt())
        val existing = reminderDao.getByContestAndRuleKey(contestId, rule.ruleKey())
        return if (existing == null) addReminder(contestId, rule) else deleteReminder(existing.id)
    }

    override suspend fun addReminder(
        contestId: String,
        rule: ReminderRule,
    ): ReminderToggleResult {
        validateReminderStructure(rule)?.let { return ReminderToggleResult.Invalid }
        val contest = contestDao.getById(contestId) ?: return ReminderToggleResult.Invalid
        val start = Instant.ofEpochMilli(contest.startTimeEpochMillis)
        when (val trigger = deriveReminderTrigger(start, rule, timeZoneProvider.currentZoneId())) {
            is DerivedReminderTrigger.Invalid -> return ReminderToggleResult.Invalid
            is DerivedReminderTrigger.Valid -> if (!trigger.triggerAt.isAfter(clock.instant())) {
                return ReminderToggleResult.TooLate
            }
        }
        val now = clock.instant()
        val zoneId = timeZoneProvider.currentZoneId()
        val inserted = database.withTransaction {
            val existingRules = reminderDao.getAllForContest(contestId).mapNotNull { it.toRule() }
            if (hasDuplicateReminder(rule, existingRules, start, now, zoneId)) {
                return@withTransaction false
            }
            val favorite = favoriteDao.get(contestId)
            if (favorite == null) {
                favoriteDao.insert(
                    FavoriteEntity(
                        contestId = contestId,
                        createdAtEpochMillis = clock.millis(),
                        note = null,
                        reminderMode = ReminderMode.CUSTOM.name,
                    ),
                )
            } else {
                favoriteDao.updateReminderMode(contestId, ReminderMode.CUSTOM.name)
            }
            reminderDao.upsert(newEntity(contestId, rule))
            true
        }
        if (!inserted) return ReminderToggleResult.Duplicate
        requestReconcile()
        return ReminderToggleResult.Scheduled(scheduler.canScheduleExact())
    }

    override suspend fun updateReminder(
        reminderId: String,
        rule: ReminderRule,
    ): ReminderToggleResult {
        validateReminderStructure(rule)?.let { return ReminderToggleResult.Invalid }
        val existing = reminderDao.getById(reminderId) ?: return ReminderToggleResult.Invalid
        val contest = contestDao.getById(existing.contestId) ?: return ReminderToggleResult.Invalid
        val trigger = deriveReminderTrigger(
            Instant.ofEpochMilli(contest.startTimeEpochMillis),
            rule,
            timeZoneProvider.currentZoneId(),
        )
        if (trigger is DerivedReminderTrigger.Invalid) return ReminderToggleResult.Invalid
        if (!(trigger as DerivedReminderTrigger.Valid).triggerAt.isAfter(clock.instant())) {
            return ReminderToggleResult.TooLate
        }
        val updated = database.withTransaction {
            val existingRules = reminderDao.getAllForContest(existing.contestId)
                .filterNot { it.id == reminderId }
                .mapNotNull { it.toRule() }
            if (hasDuplicateReminder(
                    rule,
                    existingRules,
                    Instant.ofEpochMilli(contest.startTimeEpochMillis),
                    clock.instant(),
                    timeZoneProvider.currentZoneId(),
                )
            ) {
                return@withTransaction false
            }
            favoriteDao.updateReminderMode(existing.contestId, ReminderMode.CUSTOM.name)
            reminderDao.upsert(existing.withRule(rule))
            true
        }
        if (!updated) return ReminderToggleResult.Duplicate
        requestReconcile()
        return ReminderToggleResult.Scheduled(scheduler.canScheduleExact())
    }

    override suspend fun deleteReminder(reminderId: String): ReminderToggleResult {
        val existing = reminderDao.getById(reminderId) ?: return ReminderToggleResult.Removed
        database.withTransaction {
            favoriteDao.updateReminderMode(existing.contestId, ReminderMode.CUSTOM.name)
            cleanupDao.upsert(existing.cleanup("delete"))
            reminderDao.delete(reminderId)
        }
        requestReconcile()
        return ReminderToggleResult.Removed
    }

    override suspend fun scheduleDefaultReminder(contestId: String, offset: Duration) {
        val definition = ReminderDefinition(
            id = "legacy-default-${offset.toMinutes()}",
            rule = ReminderRule.Relative(offset.toMinutes().toInt()),
            createdAt = clock.instant(),
        )
        favoriteWithDefaultReminders(contestId, listOf(definition))
    }

    override suspend fun favoriteWithDefaultReminders(
        contestId: String,
        reminders: List<ReminderDefinition>,
    ) {
        val contest = checkNotNull(contestDao.getById(contestId)) { "Contest not found: $contestId" }
        val start = Instant.ofEpochMilli(contest.startTimeEpochMillis)
        val now = clock.instant()
        val zoneId = timeZoneProvider.currentZoneId()
        val valid = reminders
            .distinctBy { it.rule.ruleKey() }
            .filter { definition ->
                val trigger = deriveReminderTrigger(start, definition.rule, zoneId)
                trigger is DerivedReminderTrigger.Valid && trigger.triggerAt.isAfter(now)
            }
        database.withTransaction {
            if (!favoriteDao.isFavorite(contestId)) {
                favoriteDao.insert(
                    FavoriteEntity(
                        contestId = contestId,
                        createdAtEpochMillis = clock.millis(),
                        note = null,
                        reminderMode = ReminderMode.DEFAULT_SNAPSHOT.name,
                    ),
                )
            }
            valid.forEach { definition ->
                if (reminderDao.getByContestAndRuleKey(contestId, definition.rule.ruleKey()) == null) {
                    reminderDao.upsert(
                        newEntity(
                            contestId = contestId,
                            rule = definition.rule,
                            id = "$contestId:${definition.id}",
                            createdAt = definition.createdAt.takeUnless { it == Instant.EPOCH } ?: now,
                        ),
                    )
                }
            }
        }
        requestReconcile()
    }

    override suspend fun removeFavoriteAndReminders(contestId: String) {
        database.withTransaction {
            val reminders = reminderDao.getAllForContest(contestId)
            cleanupDao.upsertAll(reminders.map { it.cleanup("unfavorite") })
            reminderDao.deleteForContest(contestId)
            favoriteDao.delete(contestId)
        }
        requestReconcile()
    }

    override suspend fun replaceCustomReminders(
        contestId: String,
        reminders: List<ReminderDefinition>,
    ) {
        val contest = checkNotNull(contestDao.getById(contestId)) { "Contest not found: $contestId" }
        val start = Instant.ofEpochMilli(contest.startTimeEpochMillis)
        val now = clock.instant()
        val zone = timeZoneProvider.currentZoneId()
        val valid = reminders.distinctBy { it.rule.ruleKey() }.filter { definition ->
            val trigger = deriveReminderTrigger(start, definition.rule, zone)
            trigger is DerivedReminderTrigger.Valid && trigger.triggerAt.isAfter(now)
        }
        database.withTransaction {
            val existing = reminderDao.getAllForContest(contestId)
            cleanupDao.upsertAll(existing.map { it.cleanup("replace") })
            reminderDao.deleteForContest(contestId)
            val favorite = favoriteDao.get(contestId)
            if (favorite == null) {
                favoriteDao.insert(
                    FavoriteEntity(
                        contestId = contestId,
                        createdAtEpochMillis = clock.millis(),
                        note = null,
                        reminderMode = ReminderMode.CUSTOM.name,
                    ),
                )
            } else {
                favoriteDao.updateReminderMode(contestId, ReminderMode.CUSTOM.name)
            }
            valid.forEach { definition ->
                reminderDao.upsert(
                    newEntity(
                        contestId = contestId,
                        rule = definition.rule,
                        id = if (definition.id.startsWith("$contestId:")) {
                            definition.id
                        } else {
                            "$contestId:${definition.id}"
                        },
                        createdAt = definition.createdAt.takeUnless { it == Instant.EPOCH } ?: now,
                    ),
                )
            }
        }
        requestReconcile()
    }

    override suspend fun clearForContest(contestId: String) {
        database.withTransaction {
            val reminders = reminderDao.getAllForContest(contestId)
            cleanupDao.upsertAll(reminders.map { it.cleanup("clear") })
            reminderDao.deleteForContest(contestId)
        }
        requestReconcile()
    }

    override suspend fun rescheduleForContests(contests: List<Contest>) {
        requestReconcile()
    }

    override suspend fun rescheduleAll() {
        reconcileNow()
    }

    override fun requestReconcile() {
        reconcileRequests.trySend(Unit)
    }

    override suspend fun reconcileNow() {
        reconcileMutex.withLock {
            processPendingCleanup()
            reminderDao.getAllEnabled().forEach { reminder ->
                runCatching { reconcile(reminder) }
                    .onFailure {
                        reminderDao.updateScheduleState(
                            reminderId = reminder.id,
                            triggerAtEpochMillis = reminder.triggerAtEpochMillis,
                            schedulerType = reminder.schedulerType,
                            scheduleStatus = ReminderScheduleStatus.UNSCHEDULED.name,
                            failureReason = ReminderFailureReason.SYSTEM_SCHEDULING_FAILURE.name,
                        )
                    }
            }
        }
    }

    private suspend fun processPendingCleanup() {
        cleanupDao.getAll().forEach { cleanup ->
            runCatching {
                scheduler.cancelLegacy(
                    reminderId = cleanup.reminderId,
                    requestCode = cleanup.requestCode,
                    pendingIntentVersion = cleanup.pendingIntentVersion,
                )
            }.onSuccess { cleanupDao.delete(cleanup.id) }
        }
    }

    private suspend fun reconcile(reminder: ReminderEntity) {
        val contest = contestDao.getById(reminder.contestId)
        val rule = reminder.toRule()
        if (contest == null || rule == null) {
            runCatching { scheduler.cancel(reminder) }
            reminderDao.updateScheduleState(
                reminder.id, null, reminder.schedulerType,
                ReminderScheduleStatus.INVALID.name, null,
            )
            return
        }
        val derived = deriveReminderTrigger(
            contestStart = Instant.ofEpochMilli(contest.startTimeEpochMillis),
            rule = rule,
            zoneId = timeZoneProvider.currentZoneId(),
        )
        if (derived is DerivedReminderTrigger.Invalid) {
            runCatching { scheduler.cancel(reminder) }
            reminderDao.updateScheduleState(
                reminder.id, null, reminder.schedulerType,
                ReminderScheduleStatus.INVALID.name, null,
            )
            return
        }
        val triggerAt = (derived as DerivedReminderTrigger.Valid).triggerAt
        if (!triggerAt.isAfter(clock.instant())) {
            runCatching { scheduler.cancel(reminder) }
            val alreadyFired = reminder.scheduleStatus == ReminderScheduleStatus.FIRED.name &&
                reminder.deliveryStatus != ReminderDeliveryStatus.NOT_ATTEMPTED.name &&
                reminder.triggerAtEpochMillis == triggerAt.toEpochMilli()
            reminderDao.updateScheduleState(
                reminder.id,
                triggerAt.toEpochMilli(),
                reminder.schedulerType,
                if (alreadyFired) ReminderScheduleStatus.FIRED.name else ReminderScheduleStatus.EXPIRED.name,
                null,
            )
            return
        }
        val schedulerType = scheduler.schedulerType()
        val scheduled = reminder.copy(
            triggerAtEpochMillis = triggerAt.toEpochMilli(),
            schedulerType = schedulerType,
            scheduleStatus = if (schedulerType == AndroidAlarmReminderScheduler.SCHEDULER_EXACT) {
                ReminderScheduleStatus.SCHEDULED_EXACT.name
            } else {
                ReminderScheduleStatus.SCHEDULED_INEXACT.name
            },
            failureReason = null,
        )
        try {
            scheduler.schedule(scheduled)
            reminderDao.updateScheduleState(
                scheduled.id,
                scheduled.triggerAtEpochMillis,
                scheduled.schedulerType,
                scheduled.scheduleStatus,
                null,
            )
        } catch (throwable: Throwable) {
            runCatching { scheduler.cancel(reminder) }
            reminderDao.updateScheduleState(
                reminder.id,
                triggerAt.toEpochMilli(),
                schedulerType,
                ReminderScheduleStatus.UNSCHEDULED.name,
                ReminderFailureReason.SYSTEM_SCHEDULING_FAILURE.name,
            )
        }
    }

    private fun newEntity(
        contestId: String,
        rule: ReminderRule,
        id: String = "$contestId:${UUID.randomUUID()}",
        createdAt: Instant = clock.instant(),
    ): ReminderEntity = ReminderEntity(
        id = id,
        contestId = contestId,
        triggerAtEpochMillis = null,
        offsetMinutes = (rule as? ReminderRule.Relative)?.offsetMinutes?.toLong() ?: 0L,
        ruleType = if (rule is ReminderRule.Relative) {
            ReminderEntity.RULE_RELATIVE
        } else {
            ReminderEntity.RULE_FIXED_TIME
        },
        ruleKey = rule.ruleKey(),
        fixedDayOffset = (rule as? ReminderRule.FixedTime)?.dayOffset,
        fixedHour = (rule as? ReminderRule.FixedTime)?.hour,
        fixedMinute = (rule as? ReminderRule.FixedTime)?.minute,
        isEnabled = true,
        schedulerType = scheduler.schedulerType(),
        scheduleStatus = ReminderScheduleStatus.UNSCHEDULED.name,
        deliveryStatus = ReminderDeliveryStatus.NOT_ATTEMPTED.name,
        systemRequestCode = stableReminderRequestCode(id),
        createdAtEpochMillis = createdAt.toEpochMilli(),
    )

    private fun ReminderEntity.withRule(rule: ReminderRule): ReminderEntity = copy(
        triggerAtEpochMillis = null,
        offsetMinutes = (rule as? ReminderRule.Relative)?.offsetMinutes?.toLong() ?: 0L,
        ruleType = if (rule is ReminderRule.Relative) {
            ReminderEntity.RULE_RELATIVE
        } else {
            ReminderEntity.RULE_FIXED_TIME
        },
        ruleKey = rule.ruleKey(),
        fixedDayOffset = (rule as? ReminderRule.FixedTime)?.dayOffset,
        fixedHour = (rule as? ReminderRule.FixedTime)?.hour,
        fixedMinute = (rule as? ReminderRule.FixedTime)?.minute,
        scheduleStatus = ReminderScheduleStatus.UNSCHEDULED.name,
        deliveryStatus = ReminderDeliveryStatus.NOT_ATTEMPTED.name,
        failureReason = null,
        lastDeliveryAttemptAtEpochMillis = null,
    )

    private fun ReminderEntity.toRule(): ReminderRule? {
        return when (ruleType) {
            ReminderEntity.RULE_RELATIVE -> ReminderRule.Relative(offsetMinutes.toInt())
            ReminderEntity.RULE_FIXED_TIME -> ReminderRule.FixedTime(
                dayOffset = fixedDayOffset ?: return null,
                hour = fixedHour ?: return null,
                minute = fixedMinute ?: return null,
            )
            else -> null
        }
    }

    private fun ReminderEntity.cleanup(reason: String): PendingAlarmCleanupEntity =
        PendingAlarmCleanupEntity(
            id = "$reason:$id",
            reminderId = id,
            requestCode = systemRequestCode,
            pendingIntentVersion = AndroidAlarmReminderScheduler.CURRENT_PENDING_INTENT_VERSION,
            createdAtEpochMillis = clock.millis(),
        )
}
