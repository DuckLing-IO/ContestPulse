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
        val operation = reminder.pendingIntent()
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.triggerAtEpochMillis,
                operation,
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.triggerAtEpochMillis,
                operation,
            )
        }
    }

    override fun cancel(reminder: ReminderEntity) {
        alarmManager.cancel(reminder.pendingIntent())
    }

    private fun ReminderEntity.pendingIntent(): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_FIRE_REMINDER
            data = Uri.parse("contestpulse://reminder/${Uri.encode(id)}")
            putExtra(EXTRA_REMINDER_ID, id)
        }
        return PendingIntent.getBroadcast(
            context,
            systemRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_FIRE_REMINDER = "io.duckling.contestpulse.action.FIRE_REMINDER"
        const val EXTRA_REMINDER_ID = "reminderId"
        const val SCHEDULER_EXACT = "EXACT_ALARM"
        const val SCHEDULER_INEXACT = "INEXACT_ALARM"
    }
}
