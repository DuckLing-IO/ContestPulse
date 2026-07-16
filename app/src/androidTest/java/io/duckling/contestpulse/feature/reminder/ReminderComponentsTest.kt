package io.duckling.contestpulse.feature.reminder

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.duckling.contestpulse.core.designsystem.theme.ContestPulseTheme
import java.time.Instant
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test

class ReminderComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialRelativeEditorUsesNumericDayWheelAndFixedUnit() {
        composeRule.setContent {
            ContestPulseTheme {
                AddReminderDialog(
                    visible = true,
                    editing = null,
                    existingRules = emptyList(),
                    contestStart = Instant.parse("2026-07-18T12:00:00Z"),
                    now = Instant.parse("2026-07-16T00:00:00Z"),
                    zoneId = ZoneId.of("Asia/Shanghai"),
                    onDismiss = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText("0").assertExists()
        composeRule.onNodeWithText("天前").assertExists()
        composeRule.onNodeWithText("0 天前").assertDoesNotExist()
        composeRule.onNodeWithText("小时").assertDoesNotExist()
    }

    @Test
    fun fixedModeKeepsThreeUnitsOutsideNumericWheels() {
        composeRule.setContent {
            ContestPulseTheme {
                AddReminderDialog(
                    visible = true,
                    editing = null,
                    existingRules = emptyList(),
                    contestStart = Instant.parse("2026-07-18T12:00:00Z"),
                    now = Instant.parse("2026-07-16T00:00:00Z"),
                    zoneId = ZoneId.of("Asia/Shanghai"),
                    onDismiss = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText("固定时间").performClick()
        composeRule.onNodeWithText("天前").assertExists()
        composeRule.onNodeWithText("时").assertExists()
        composeRule.onNodeWithText("分").assertExists()
        composeRule.onNodeWithText("00 时").assertDoesNotExist()
        check(composeRule.onAllNodesWithText("00").fetchSemanticsNodes().isNotEmpty())
    }
}
