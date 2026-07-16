package io.duckling.contestpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.duckling.contestpulse.core.designsystem.theme.ContestPulseTheme
import io.duckling.contestpulse.navigation.ContestPulseApp
import io.duckling.contestpulse.domain.reminder.ReminderManager
import io.duckling.contestpulse.core.time.MinuteTicker
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var reminderManager: ReminderManager
    @Inject lateinit var minuteTicker: MinuteTicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContestPulseTheme {
                ContestPulseApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        minuteTicker.refresh()
        reminderManager.requestReconcile()
    }
}
