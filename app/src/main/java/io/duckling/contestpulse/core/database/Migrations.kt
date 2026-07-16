package io.duckling.contestpulse.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pending_alarm_cleanup (
                id TEXT NOT NULL PRIMARY KEY,
                reminderId TEXT NOT NULL,
                requestCode INTEGER NOT NULL,
                pendingIntentVersion INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT OR IGNORE INTO pending_alarm_cleanup
                (id, reminderId, requestCode, pendingIntentVersion, createdAtEpochMillis)
            SELECT 'migration:' || duplicate.id,
                   duplicate.id,
                   duplicate.systemRequestCode,
                   1,
                   duplicate.createdAtEpochMillis
            FROM reminders AS duplicate
            WHERE EXISTS (
                SELECT 1 FROM reminders AS keeper
                WHERE keeper.contestId = duplicate.contestId
                  AND keeper.offsetMinutes = duplicate.offsetMinutes
                  AND (
                      keeper.createdAtEpochMillis < duplicate.createdAtEpochMillis OR
                      (keeper.createdAtEpochMillis = duplicate.createdAtEpochMillis AND keeper.id < duplicate.id)
                  )
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT OR IGNORE INTO pending_alarm_cleanup
                (id, reminderId, requestCode, pendingIntentVersion, createdAtEpochMillis)
            SELECT 'migration-legacy:' || id,
                   id,
                   systemRequestCode,
                   1,
                   createdAtEpochMillis
            FROM reminders
            """.trimIndent(),
        )
        db.execSQL(
            "ALTER TABLE favorites ADD COLUMN reminderMode TEXT NOT NULL DEFAULT 'CUSTOM'",
        )
        db.execSQL(
            """
            CREATE TABLE reminders_new (
                id TEXT NOT NULL PRIMARY KEY,
                contestId TEXT NOT NULL,
                triggerAtEpochMillis INTEGER,
                offsetMinutes INTEGER NOT NULL,
                ruleType TEXT NOT NULL,
                ruleKey TEXT NOT NULL,
                fixedDayOffset INTEGER,
                fixedHour INTEGER,
                fixedMinute INTEGER,
                isEnabled INTEGER NOT NULL,
                schedulerType TEXT NOT NULL,
                scheduleStatus TEXT NOT NULL,
                deliveryStatus TEXT NOT NULL,
                failureReason TEXT,
                lastDeliveryAttemptAtEpochMillis INTEGER,
                systemRequestCode INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                FOREIGN KEY(contestId) REFERENCES contests(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO reminders_new (
                id, contestId, triggerAtEpochMillis, offsetMinutes, ruleType, ruleKey,
                fixedDayOffset, fixedHour, fixedMinute, isEnabled, schedulerType,
                scheduleStatus, deliveryStatus, failureReason, lastDeliveryAttemptAtEpochMillis,
                systemRequestCode, createdAtEpochMillis
            )
            SELECT source.id, source.contestId, source.triggerAtEpochMillis, source.offsetMinutes,
                   'RELATIVE', 'relative:' || source.offsetMinutes,
                   NULL, NULL, NULL, source.isEnabled, source.schedulerType,
                   CASE WHEN source.offsetMinutes = 0 THEN 'INVALID' ELSE 'UNSCHEDULED' END,
                   'NOT_ATTEMPTED', NULL, NULL, source.systemRequestCode, source.createdAtEpochMillis
            FROM reminders AS source
            WHERE NOT EXISTS (
                SELECT 1 FROM reminders AS preferred
                WHERE preferred.contestId = source.contestId
                  AND preferred.offsetMinutes = source.offsetMinutes
                  AND (
                      preferred.createdAtEpochMillis < source.createdAtEpochMillis OR
                      (preferred.createdAtEpochMillis = source.createdAtEpochMillis AND preferred.id < source.id)
                  )
            )
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE reminders")
        db.execSQL("ALTER TABLE reminders_new RENAME TO reminders")
        db.execSQL("CREATE INDEX index_reminders_contestId ON reminders(contestId)")
        db.execSQL(
            "CREATE UNIQUE INDEX index_reminders_contestId_ruleKey ON reminders(contestId, ruleKey)",
        )
        db.execSQL(
            "CREATE INDEX index_reminders_triggerAtEpochMillis ON reminders(triggerAtEpochMillis)",
        )
    }
}
