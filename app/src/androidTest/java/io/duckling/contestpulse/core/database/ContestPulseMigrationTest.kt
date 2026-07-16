package io.duckling.contestpulse.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContestPulseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ContestPulseDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrationOneToTwoDeduplicatesBeforeUniqueIndexAndQueuesLegacyCleanup() {
        helper.createDatabase(DATABASE_NAME, 1).use { database ->
            database.execSQL(
                """
                INSERT INTO contests (
                    id, source, sourceContestId, title, startTimeEpochMillis,
                    endTimeEpochMillis, durationMinutes, registrationUrl, contestUrl,
                    status, category, difficultyLabel, ratedRange, isRated,
                    lastUpdatedAtEpochMillis, remoteFingerprint
                ) VALUES ('contest', 'ATCODER', '467', 'Contest', 2000000,
                    NULL, NULL, NULL, 'https://example.com', 'UPCOMING', NULL,
                    NULL, NULL, NULL, 1000, 'fingerprint')
                """.trimIndent(),
            )
            database.execSQL(
                "INSERT INTO favorites (contestId, createdAtEpochMillis, note) VALUES ('contest', 1000, NULL)",
            )
            database.execSQL("DROP INDEX index_reminders_contestId_offsetMinutes")
            insertLegacyReminder(database, "later-id", 2000, 22)
            insertLegacyReminder(database, "earlier-id", 1000, 11)
        }

        helper.runMigrationsAndValidate(DATABASE_NAME, 2, true, MIGRATION_1_2).use { database ->
            database.query(
                "SELECT id, ruleKey, scheduleStatus FROM reminders WHERE contestId = 'contest'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("earlier-id", cursor.getString(0))
                assertEquals("relative:60", cursor.getString(1))
                assertEquals("UNSCHEDULED", cursor.getString(2))
                assertTrue(!cursor.moveToNext())
            }
            database.query(
                "SELECT COUNT(*) FROM pending_alarm_cleanup WHERE reminderId = 'later-id'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(2, cursor.getInt(0))
            }
            database.query("SELECT reminderMode FROM favorites WHERE contestId = 'contest'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("CUSTOM", cursor.getString(0))
            }
        }
    }

    private fun insertLegacyReminder(
        database: androidx.sqlite.db.SupportSQLiteDatabase,
        id: String,
        createdAt: Long,
        requestCode: Int,
    ) {
        database.execSQL(
            """
            INSERT INTO reminders (
                id, contestId, triggerAtEpochMillis, offsetMinutes, isEnabled,
                schedulerType, systemRequestCode, createdAtEpochMillis
            ) VALUES ('$id', 'contest', 1500000, 60, 1, 'EXACT_ALARM', $requestCode, $createdAt)
            """.trimIndent(),
        )
    }

    private companion object {
        const val DATABASE_NAME = "contest-pulse-migration-test"
    }
}
