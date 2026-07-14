package io.duckling.contestpulse.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.duckling.contestpulse.core.database.entity.FavoriteEntity
import io.duckling.contestpulse.core.database.entity.ContestEntity
import io.duckling.contestpulse.core.database.entity.ReminderEntity
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContestPulseDatabaseTest {
    private lateinit var database: ContestPulseDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ContestPulseDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun contestUpsert_updatesRemoteFields() = runBlocking {
        val original = testContestEntity("upsert")
        database.contestDao().upsertAll(listOf(original))
        database.contestDao().upsertAll(listOf(original.copy(title = "Updated title")))

        assertEquals("Updated title", database.contestDao().getById(original.id)?.title)
    }

    @Test
    fun favoriteAndReminderRelations_areObservedAndCascadeWithContest() = runBlocking {
        val contest = testContestEntity("relations")
        database.contestDao().upsertAll(listOf(contest))
        database.favoriteDao().insert(
            FavoriteEntity(contest.id, createdAtEpochMillis = 1L, note = null),
        )
        database.reminderDao().upsert(
            ReminderEntity(
                id = "${contest.id}:60",
                contestId = contest.id,
                triggerAtEpochMillis = contest.startTimeEpochMillis - 3_600_000L,
                offsetMinutes = 60,
                isEnabled = true,
                schedulerType = "TEST",
                systemRequestCode = 1,
                createdAtEpochMillis = 1L,
            ),
        )

        val related = database.contestDao().observeByIdWithFavorite(contest.id).first()
        requireNotNull(related)
        assertEquals(contest.id, related.favorite?.contestId)
        assertEquals(1, related.reminders.size)

        database.contestDao().deleteById(contest.id)
        assertFalse(database.favoriteDao().isFavorite(contest.id))
        assertNull(database.reminderDao().getById("${contest.id}:60"))
    }

    @Test
    fun cleanup_keepsFavoritesAndDeletesExpiredUnfavoritedContest() = runBlocking {
        val expired = testContestEntity("expired").copy(
            endTimeEpochMillis = Instant.parse("2026-06-01T00:00:00Z").toEpochMilli(),
        )
        val favorite = testContestEntity("favorite").copy(
            endTimeEpochMillis = expired.endTimeEpochMillis,
        )
        database.contestDao().upsertAll(listOf(expired, favorite))
        database.favoriteDao().insert(
            FavoriteEntity(favorite.id, createdAtEpochMillis = 1L, note = null),
        )

        database.contestDao().deleteExpiredUnfavorited(
            Instant.parse("2026-07-01T00:00:00Z").toEpochMilli(),
        )

        assertNull(database.contestDao().getById(expired.id))
        assertEquals(favorite.id, database.contestDao().getById(favorite.id)?.id)
    }

    @Test
    fun customSourceCleanup_doesNotTouchAnotherCustomSource() = runBlocking {
        val first = testContestEntity("first").copy(
            id = "custom:source-a:first",
            source = "OTHER",
            sourceContestId = "source-a:first",
            lastUpdatedAtEpochMillis = 1L,
        )
        val second = testContestEntity("second").copy(
            id = "custom:source-b:second",
            source = "OTHER",
            sourceContestId = "source-b:second",
            lastUpdatedAtEpochMillis = 1L,
        )
        database.contestDao().upsertAll(listOf(first, second))

        database.contestDao().deleteStaleUnfavoritedForCustomSource(
            sourceContestIdPrefix = "source-a:",
            refreshEpochMillis = 2L,
        )

        assertNull(database.contestDao().getById(first.id))
        assertEquals(second.id, database.contestDao().getById(second.id)?.id)
    }
}

private fun testContestEntity(sourceContestId: String): ContestEntity = ContestEntity(
    id = "codeforces:$sourceContestId",
    source = "CODEFORCES",
    sourceContestId = sourceContestId,
    title = "Test contest $sourceContestId",
    startTimeEpochMillis = Instant.parse("2026-07-14T12:00:00Z").toEpochMilli(),
    endTimeEpochMillis = Instant.parse("2026-07-14T14:00:00Z").toEpochMilli(),
    durationMinutes = 120,
    registrationUrl = null,
    contestUrl = "https://codeforces.com/contest/1",
    status = "UPCOMING",
    category = "CF",
    difficultyLabel = null,
    ratedRange = null,
    isRated = null,
    lastUpdatedAtEpochMillis = Instant.parse("2026-07-14T10:00:00Z").toEpochMilli(),
    remoteFingerprint = sourceContestId,
)
