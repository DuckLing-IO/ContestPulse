package io.duckling.contestpulse.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.duckling.contestpulse.core.database.dao.ContestDao
import io.duckling.contestpulse.core.database.dao.ReminderDao
import io.duckling.contestpulse.domain.model.ReminderDeliveryStatus
import io.duckling.contestpulse.domain.model.ReminderFailureReason
import java.time.Clock
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
    @Inject lateinit var clock: Clock

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
                val delivered = if (contest == null) {
                    false
                } else {
                    try {
                        notifier.show(contest, reminder)
                    } catch (_: Exception) {
                        false
                    }
                }
                reminderDao.updateDeliveryState(
                    reminderId = reminder.id,
                    deliveryStatus = if (delivered) {
                        ReminderDeliveryStatus.DELIVERED.name
                    } else {
                        ReminderDeliveryStatus.DELIVERY_FAILED.name
                    },
                    failureReason = if (delivered) {
                        null
                    } else {
                        ReminderFailureReason.DELIVERY_FAILURE.name
                    },
                    attemptedAt = clock.millis(),
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
