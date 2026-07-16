package io.duckling.contestpulse.reminder

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.duckling.contestpulse.core.database.ContestPulseDatabase
import io.duckling.contestpulse.core.database.entity.ContestEntity
import io.duckling.contestpulse.core.database.entity.ReminderEntity
import io.duckling.contestpulse.core.time.TimeZoneProvider
import io.duckling.contestpulse.domain.model.ReminderRule
import io.duckling.contestpulse.domain.model.ReminderScheduleStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DefaultReminderManagerTest {
    private lateinit var database: ContestPulseDatabase
    private lateinit var scheduler: RecordingScheduler
    private lateinit var manager: DefaultReminderManager
    private val now = Instant.parse("2026-07-16T00:00:00Z")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ContestPulseDatabase::class.java,
        ).allowMainThreadQueries().build()
        scheduler = RecordingScheduler()
        manager = DefaultReminderManager(
            database,
            database.contestDao(),
            database.favoriteDao(),
            database.reminderDao(),
            database.pendingAlarmCleanupDao(),
            scheduler,
            Clock.fixed(now, ZoneOffset.UTC),
            TimeZoneProvider { ZoneId.of("Asia/Shanghai") },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun reconcileRecomputesDerivedTriggerAndNeverDeletesExpiredOrFailedRules() = runBlocking {
        var contest = contestAt(now.plusSeconds(3 * 60 * 60L))
        database.contestDao().upsertAll(listOf(contest))
        manager.addReminder(contest.id, ReminderRule.Relative(60))
        manager.reconcileNow()

        val reminderId = database.reminderDao().getAllForContest(contest.id).single().id
        assertEquals(
            now.plusSeconds(2 * 60 * 60L).toEpochMilli(),
            database.reminderDao().getById(reminderId)?.triggerAtEpochMillis,
        )

        contest = contest.copy(startTimeEpochMillis = now.plusSeconds(4 * 60 * 60L).toEpochMilli())
        database.contestDao().upsertAll(listOf(contest))
        manager.reconcileNow()
        assertEquals(
            now.plusSeconds(3 * 60 * 60L).toEpochMilli(),
            database.reminderDao().getById(reminderId)?.triggerAtEpochMillis,
        )

        scheduler.failScheduling = true
        contest = contest.copy(startTimeEpochMillis = now.plusSeconds(5 * 60 * 60L).toEpochMilli())
        database.contestDao().upsertAll(listOf(contest))
        manager.reconcileNow()
        assertEquals(
            ReminderScheduleStatus.UNSCHEDULED.name,
            database.reminderDao().getById(reminderId)?.scheduleStatus,
        )
        assertNotNull(database.reminderDao().getById(reminderId))

        scheduler.failScheduling = false
        contest = contest.copy(startTimeEpochMillis = now.plusSeconds(30 * 60L).toEpochMilli())
        database.contestDao().upsertAll(listOf(contest))
        manager.reconcileNow()
        assertEquals(
            ReminderScheduleStatus.EXPIRED.name,
            database.reminderDao().getById(reminderId)?.scheduleStatus,
        )
        assertNotNull(database.reminderDao().getById(reminderId))
    }

    private fun contestAt(start: Instant) = ContestEntity(
        id = "atcoder:467",
        source = "ATCODER",
        sourceContestId = "467",
        title = "Contest",
        startTimeEpochMillis = start.toEpochMilli(),
        endTimeEpochMillis = start.plusSeconds(2 * 60 * 60L).toEpochMilli(),
        durationMinutes = 120,
        registrationUrl = null,
        contestUrl = "https://example.com",
        status = "UPCOMING",
        category = null,
        difficultyLabel = null,
        ratedRange = null,
        isRated = null,
        lastUpdatedAtEpochMillis = now.toEpochMilli(),
        remoteFingerprint = "fingerprint",
    )
}

private class RecordingScheduler : ReminderScheduler {
    var failScheduling = false

    override fun canScheduleExact(): Boolean = true
    override fun schedulerType(): String = AndroidAlarmReminderScheduler.SCHEDULER_EXACT
    override fun schedule(reminder: ReminderEntity) {
        if (failScheduling) error("simulated scheduling failure")
    }
    override fun cancel(reminder: ReminderEntity) = Unit
    override fun cancelLegacy(reminderId: String, requestCode: Int, pendingIntentVersion: Int) = Unit
}
