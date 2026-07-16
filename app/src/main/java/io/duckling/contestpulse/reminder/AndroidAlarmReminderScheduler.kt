package io.duckling.contestpulse.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import io.duckling.contestpulse.core.database.entity.ReminderEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAlarmReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReminderScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    override fun schedulerType(): String = if (canScheduleExact()) {
        SCHEDULER_EXACT
    } else {
        SCHEDULER_INEXACT
    }

    override fun schedule(reminder: ReminderEntity) {
        val triggerAtEpochMillis = requireNotNull(reminder.triggerAtEpochMillis) {
            "Cannot schedule reminder without a trigger time"
        }
        val operation = reminder.pendingIntent()
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtEpochMillis,
                operation,
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtEpochMillis,
                operation,
            )
        }
    }

    override fun cancel(reminder: ReminderEntity) {
        alarmManager.cancel(reminder.pendingIntent())
    }

    override fun cancelLegacy(reminderId: String, requestCode: Int, pendingIntentVersion: Int) {
        val intent = reminderIntent(reminderId, pendingIntentVersion)
        val operation = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(operation)
        operation.cancel()
    }

    private fun ReminderEntity.pendingIntent(): PendingIntent {
        val intent = reminderIntent(id, CURRENT_PENDING_INTENT_VERSION)
        return PendingIntent.getBroadcast(
            context,
            systemRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun reminderIntent(reminderId: String, pendingIntentVersion: Int): Intent =
        Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_FIRE_REMINDER
            data = Uri.parse(reminderPendingIntentData(reminderId, pendingIntentVersion))
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }

    companion object {
        const val ACTION_FIRE_REMINDER = "io.duckling.contestpulse.action.FIRE_REMINDER"
        const val EXTRA_REMINDER_ID = "reminderId"
        const val SCHEDULER_EXACT = "EXACT_ALARM"
        const val SCHEDULER_INEXACT = "INEXACT_ALARM"
        const val CURRENT_PENDING_INTENT_VERSION = 2
    }
}

internal fun reminderPendingIntentData(reminderId: String, pendingIntentVersion: Int): String {
    require(reminderId.isNotBlank()) { "Reminder id must not be blank" }
    require(pendingIntentVersion > 0) { "PendingIntent version must be positive" }
    val encodedId = reminderId.toByteArray(Charsets.UTF_8).joinToString("") { byte ->
        val unsigned = byte.toInt() and 0xff
        val character = unsigned.toChar()
        if (character.isAsciiUriUnreserved()) {
            character.toString()
        } else {
            "%${unsigned.toString(16).uppercase().padStart(2, '0')}"
        }
    }
    return if (pendingIntentVersion == 1) {
        "contestpulse://reminder/$encodedId"
    } else {
        "contestpulse://reminder/$encodedId/v$pendingIntentVersion"
    }
}

private fun Char.isAsciiUriUnreserved(): Boolean =
    this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9' || this in "-._~"
