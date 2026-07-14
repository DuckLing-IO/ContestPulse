package io.duckling.contestpulse.feature

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import io.duckling.contestpulse.core.designsystem.theme.ContestPulseTheme
import io.duckling.contestpulse.domain.logic.stableContestId
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestFilter
import io.duckling.contestpulse.domain.model.ContestGroup
import io.duckling.contestpulse.domain.model.ContestGroupType
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.model.ContestStatus
import io.duckling.contestpulse.domain.model.SyncErrorType
import io.duckling.contestpulse.feature.contestlist.ContestListScreen
import io.duckling.contestpulse.feature.contestlist.ContestListUiState
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StageTwoScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val now = Instant.parse("2026-07-14T10:00:00Z")
    private val contest = previewContest(now)

    @Test
    fun contestList_showsHeaderHeroAndTimeline() {
        val timelineContest = previewContest(
            now = now,
            sourceContestId = "timeline",
            title = "演示 · Timeline Contest",
            startOffset = Duration.ofHours(5),
        )
        composeRule.setContent {
            ContestPulseTheme(darkTheme = false) {
                ContestListScreen(
                    uiState = ContestListUiState(
                        isLoading = false,
                        groups = listOf(
                            ContestGroup(
                                type = ContestGroupType.TODAY,
                                contests = listOf(timelineContest),
                            ),
                        ),
                        nextContest = contest,
                        now = now,
                        lastUpdatedAt = now,
                    ),
                    onContestClick = {},
                    onToggleFavorite = {},
                    sharedElementModifier = { Modifier },
                )
            }
        }

        composeRule.onNodeWithText("赛程脉搏").assertIsDisplayed()
        composeRule.onNodeWithText(contest.title).assertIsDisplayed()
        composeRule.onNodeWithText(timelineContest.title).assertIsDisplayed()
    }

    @Test
    fun contestList_favoriteButtonEmitsAction() {
        var clicked = false
        composeRule.setContent {
            ContestPulseTheme(darkTheme = false) {
                ContestListScreen(
                    uiState = ContestListUiState(
                        isLoading = false,
                        groups = listOf(
                            ContestGroup(
                                type = ContestGroupType.TODAY,
                                contests = listOf(contest),
                            ),
                        ),
                        now = now,
                    ),
                    onContestClick = {},
                    onToggleFavorite = { clicked = true },
                    sharedElementModifier = { Modifier },
                )
            }
        }

        composeRule.onNodeWithContentDescription("收藏比赛").performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }

    @Test
    fun contestList_emptyStateIsVisibleAfterCompletedSync() {
        composeRule.setContent {
            ContestPulseTheme(darkTheme = false) {
                ContestListScreen(
                    uiState = ContestListUiState(
                        isLoading = false,
                        now = now,
                        hasCompletedSync = true,
                    ),
                    onContestClick = {},
                    onToggleFavorite = {},
                    sharedElementModifier = { Modifier },
                )
            }
        }

        composeRule.onNodeWithText("暂无即将开始的比赛").assertIsDisplayed()
    }

    @Test
    fun contestList_errorStateKeepsRetryAction() {
        var refreshed = false
        composeRule.setContent {
            ContestPulseTheme(darkTheme = false) {
                ContestListScreen(
                    uiState = ContestListUiState(
                        isLoading = false,
                        now = now,
                        syncIssueType = SyncErrorType.NETWORK,
                        hasCompletedSync = true,
                    ),
                    onContestClick = {},
                    onToggleFavorite = {},
                    onRefresh = { refreshed = true },
                    sharedElementModifier = { Modifier },
                )
            }
        }

        composeRule.onNodeWithText("同步失败 · 本地缓存未受影响").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("刷新").performClick()
        composeRule.runOnIdle { assertTrue(refreshed) }
    }

    @Test
    fun contestList_pullDownEmitsRefresh() {
        var refreshed = false
        composeRule.setContent {
            ContestPulseTheme(darkTheme = false) {
                ContestListScreen(
                    uiState = ContestListUiState(
                        isLoading = false,
                        now = now,
                        hasCompletedSync = true,
                    ),
                    onContestClick = {},
                    onToggleFavorite = {},
                    onRefresh = { refreshed = true },
                    sharedElementModifier = { Modifier },
                )
            }
        }

        composeRule.onRoot().performTouchInput { swipeDown() }
        composeRule.runOnIdle { assertTrue(refreshed) }
    }

    @Test
    fun contestFilter_ratedChipEmitsActionInDarkTheme() {
        var clicked = false
        composeRule.setContent {
            ContestPulseTheme(darkTheme = true) {
                ContestListScreen(
                    uiState = ContestListUiState(
                        isLoading = false,
                        isFilterExpanded = true,
                        filter = ContestFilter(),
                        now = now,
                    ),
                    onContestClick = {},
                    onToggleFavorite = {},
                    sharedElementModifier = { Modifier },
                    onToggleSource = {},
                    onToggleRatedOnly = { clicked = true },
                    onToggleFavoriteOnly = {},
                    onSelectDateRange = {},
                )
            }
        }

        composeRule.onNodeWithText("只看 Rated").performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }
}

private fun previewContest(
    now: Instant,
    sourceContestId: String = "preview",
    title: String = "演示 · Preview Contest",
    startOffset: Duration = Duration.ofHours(2),
): Contest {
    val startTime = now.plus(startOffset)
    val duration = Duration.ofHours(2)
    return Contest(
        id = stableContestId(ContestSource.CODEFORCES, sourceContestId),
        source = ContestSource.CODEFORCES,
        sourceContestId = sourceContestId,
        title = title,
        startTime = startTime,
        endTime = startTime.plus(duration),
        duration = duration,
        registrationUrl = null,
        contestUrl = "https://codeforces.com/contests",
        status = ContestStatus.UPCOMING,
        category = "Demo",
        difficultyLabel = "Rated",
        ratedRange = "0 - 2100",
        isRated = true,
        isFavorite = false,
        reminderOffsets = emptySet(),
        lastUpdatedAt = now,
    )
}
