package io.duckling.contestpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.duckling.contestpulse.core.designsystem.theme.ContestPulseTheme
import io.duckling.contestpulse.navigation.ContestPulseApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContestPulseTheme {
                ContestPulseApp()
            }
        }
    }
}
