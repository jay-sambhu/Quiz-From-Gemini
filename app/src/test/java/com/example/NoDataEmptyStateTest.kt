package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.components.EmptyDataState
import com.example.ui.components.NoLeaderboardEmptyState
import com.example.ui.components.NoQuizResultsEmptyState
import com.example.ui.components.NoTeacherAnalyticsEmptyState
import com.example.ui.theme.QuizPlatformTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NoDataEmptyStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testNoQuizResultsEmptyState_displaysTitleAndTriggersAction() {
        var actionClicked = false

        composeTestRule.setContent {
            QuizPlatformTheme {
                NoQuizResultsEmptyState(
                    onTakeQuiz = { actionClicked = true },
                    testTag = "test_no_quiz_empty"
                )
            }
        }

        // Verify title & description exist
        composeTestRule.onNodeWithText("No Quiz Results Yet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("test_no_quiz_empty").assertIsDisplayed()

        // Verify action button can be tapped
        val actionButton = composeTestRule.onNodeWithTag("test_no_quiz_empty_action_btn")
        actionButton.assertIsDisplayed()
        actionButton.performClick()

        assertTrue("Action button callback should be invoked", actionClicked)
    }

    @Test
    fun testNoLeaderboardEmptyState_displaysTitleAndTriggersAction() {
        var actionClicked = false

        composeTestRule.setContent {
            QuizPlatformTheme {
                NoLeaderboardEmptyState(
                    onTakeQuizToClimb = { actionClicked = true },
                    testTag = "test_no_leaderboard_empty"
                )
            }
        }

        composeTestRule.onNodeWithText("No Leaderboard Entries Yet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("test_no_leaderboard_empty").assertIsDisplayed()

        val actionButton = composeTestRule.onNodeWithTag("test_no_leaderboard_empty_action_btn")
        actionButton.assertIsDisplayed()
        actionButton.performClick()

        assertTrue("Take Quiz to Climb callback should be invoked", actionClicked)
    }

    @Test
    fun testNoTeacherAnalyticsEmptyState_displaysMessage() {
        composeTestRule.setContent {
            QuizPlatformTheme {
                NoTeacherAnalyticsEmptyState(
                    testTag = "test_teacher_empty"
                )
            }
        }

        composeTestRule.onNodeWithText("No Quiz Results Submitted Yet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("test_teacher_empty").assertIsDisplayed()
    }

    @Test
    fun testGenericEmptyDataState_supportsCustomActionsAndBadge() {
        var primaryClicked = false
        var secondaryClicked = false

        composeTestRule.setContent {
            QuizPlatformTheme {
                EmptyDataState(
                    icon = Icons.Default.Quiz,
                    title = "Custom Empty State",
                    description = "Custom descriptive explanation here.",
                    badgeText = "Awaiting Action",
                    actionLabel = "Primary Action",
                    onActionClick = { primaryClicked = true },
                    secondaryActionLabel = "Secondary Option",
                    onSecondaryActionClick = { secondaryClicked = true },
                    testTag = "custom_empty_state"
                )
            }
        }

        composeTestRule.onNodeWithText("Custom Empty State").assertIsDisplayed()
        composeTestRule.onNodeWithText("Custom descriptive explanation here.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Awaiting Action").assertIsDisplayed()

        composeTestRule.onNodeWithTag("custom_empty_state_action_btn").performClick()
        assertEquals(true, primaryClicked)

        composeTestRule.onNodeWithTag("custom_empty_state_secondary_btn").performClick()
        assertEquals(true, secondaryClicked)
    }
}
