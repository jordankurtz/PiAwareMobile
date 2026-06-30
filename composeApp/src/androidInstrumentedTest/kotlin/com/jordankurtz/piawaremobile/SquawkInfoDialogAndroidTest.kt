package com.jordankurtz.piawaremobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jordankurtz.piawaremobile.squawk.ui.SquawkInfoDialog
import com.jordankurtz.piawaremobile.ui.Theme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SquawkInfoDialogAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun knownCodeShowsNameAndDescription() {
        composeRule.setContent {
            Theme {
                SquawkInfoDialog(squawk = "7700", onDismiss = {})
            }
        }
        composeRule.onNodeWithText("7700").assertIsDisplayed()
        composeRule.onNodeWithText("General Emergency").assertIsDisplayed()
    }

    @Test
    fun unknownCodeShowsFallbackText() {
        composeRule.setContent {
            Theme {
                SquawkInfoDialog(squawk = "9999", onDismiss = {})
            }
        }
        composeRule.onNodeWithText("9999").assertIsDisplayed()
        composeRule.onNodeWithText("Unknown Code").assertIsDisplayed()
    }

    @Test
    fun dismissButtonFiresCallback() {
        var dismissed = false
        composeRule.setContent {
            Theme {
                SquawkInfoDialog(squawk = "7700", onDismiss = { dismissed = true })
            }
        }
        composeRule.onNodeWithText("Dismiss").performClick()
        assertTrue(dismissed)
    }
}
