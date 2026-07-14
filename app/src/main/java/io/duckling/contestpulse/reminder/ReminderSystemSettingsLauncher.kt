package io.duckling.contestpulse.reminder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

fun Context.openReminderSystemSettings(
    notificationsEnabled: Boolean,
    exactRemindersAvailable: Boolean,
): Boolean {
    val packageUri = Uri.parse("package:$packageName")
    val intent = when {
        !notificationsEnabled ->
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        !exactRemindersAvailable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri)
        else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
    }
    return runCatching { startActivity(intent) }.isSuccess
}
