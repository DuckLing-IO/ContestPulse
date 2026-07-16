package io.duckling.contestpulse.reminder

import io.duckling.contestpulse.core.database.entity.ReminderEntity

interface ReminderScheduler {
    fun canScheduleExact(): Boolean

    fun schedulerType(): String

    fun schedule(reminder: ReminderEntity)

    fun cancel(reminder: ReminderEntity)

    fun cancelLegacy(reminderId: String, requestCode: Int, pendingIntentVersion: Int)
}
