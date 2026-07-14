package io.duckling.contestpulse.feature

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.duckling.contestpulse.core.designsystem.theme.ContestPulseTheme
import io.duckling.contestpulse.feature.customsource.CustomSourcesScreen
import io.duckling.contestpulse.feature.customsource.CustomSourcesUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CustomSourcesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyList_explainsPreviewAndEmitsAddAction() {
        var addClicked = false
        composeRule.setContent {
            ContestPulseTheme(darkTheme = false) {
                CustomSourcesScreen(
                    uiState = CustomSourcesUiState(isLoading = false),
                    onBack = {},
                    onAdd = { addClicked = true },
                    onEdit = {},
                    onSetEnabled = { _, _ -> },
                    onRequestDelete = {},
                    onCancelDelete = {},
                    onConfirmDelete = {},
                    onNameChange = {},
                    onUrlChange = {},
                    onTimezoneChange = {},
                    onFormatChange = {},
                    onToggleAdvanced = {},
                    onItemSelectorChange = {},
                    onTitleSelectorChange = {},
                    onStartSelectorChange = {},
                    onEndSelectorChange = {},
                    onLinkSelectorChange = {},
                    onDateTimePatternChange = {},
                    onPreview = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText("自定义来源").assertIsDisplayed()
        composeRule.onNodeWithText("添加数据源").performClick()
        composeRule.runOnIdle { assertTrue(addClicked) }
    }
}
