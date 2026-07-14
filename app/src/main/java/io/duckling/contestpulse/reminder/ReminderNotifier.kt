package io.duckling.contestpulse.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.duckling.contestpulse.MainActivity
import io.duckling.contestpulse.R
import io.duckling.contestpulse.core.database.entity.ContestEntity
import io.duckling.contestpulse.core.database.entity.ReminderEntity
import io.duckling.contestpulse.navigation.ContestDetailDestination
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            },
        )
    }

    fun show(
        contest: ContestEntity,
        reminder: ReminderEntity,
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val contentIntent = Intent(
            Intent.ACTION_VIEW,
            ContestDetailDestination.deepLinkUri(contest.id),
            context,
            MainActivity::class.java,
        )
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminder.systemRequestCode,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_contests)
            .setContentTitle(
                context.getString(R.string.notification_title, contest.title),
            )
            .setContentText(reminderBody(reminder.offsetMinutes))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(
            reminder.systemRequestCode,
            notification,
        )
        return true
    }

    private fun reminderBody(offsetMinutes: Long): String = when {
        offsetMinutes == 0L -> context.getString(R.string.notification_body_now)
        offsetMinutes % MINUTES_PER_DAY == 0L -> context.getString(
            R.string.notification_body_days,
            offsetMinutes / MINUTES_PER_DAY,
        )
        offsetMinutes % MINUTES_PER_HOUR == 0L -> context.getString(
            R.string.notification_body_hours,
            offsetMinutes / MINUTES_PER_HOUR,
        )
        else -> context.getString(R.string.notification_body_minutes, offsetMinutes)
    }

    companion object {
        const val CHANNEL_ID = "contest_reminders"
    }
}

private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 1_440L
