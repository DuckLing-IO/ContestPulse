package io.duckling.contestpulse.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.duckling.contestpulse.core.database.dao.ContestDao
import io.duckling.contestpulse.core.database.dao.ReminderDao
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var reminderDao: ReminderDao
    @Inject lateinit var contestDao: ContestDao
    @Inject lateinit var notifier: ReminderNotifier

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidAlarmReminderScheduler.ACTION_FIRE_REMINDER) return
        val reminderId = intent.getStringExtra(AndroidAlarmReminderScheduler.EXTRA_REMINDER_ID)
            ?: return
        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                val reminder = reminderDao.getById(reminderId) ?: return@launch
                if (!reminder.isEnabled) return@launch
                val contest = contestDao.getById(reminder.contestId)
                if (contest != null) {
                    notifier.show(contest, reminder)
                }
                reminderDao.delete(reminder.id)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
