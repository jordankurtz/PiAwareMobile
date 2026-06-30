package com.jordankurtz.piawaremobile.squawk

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.squawk.ui.SquawkInfoDialog
import com.jordankurtz.piawaremobile.ui.Theme
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SquawkInfoDialogTest {
    @Test
    fun `known code shows name and description`() =
        runComposeUiTest {
            setContent {
                Theme {
                    SquawkInfoDialog(squawk = "7700", onDismiss = {})
                }
            }
            onNodeWithText("7700").assertExists()
            onNodeWithText("General Emergency").assertExists()
            onNodeWithText("Declared emergency; pilot requires immediate ATC assistance.").assertExists()
        }

    @Test
    fun `unknown code shows fallback text`() =
        runComposeUiTest {
            setContent {
                Theme {
                    SquawkInfoDialog(squawk = "9999", onDismiss = {})
                }
            }
            onNodeWithText("9999").assertExists()
            onNodeWithText("Unknown Code").assertExists()
            onNodeWithText("No specific meaning is assigned to this squawk code.").assertExists()
        }

    @Test
    fun `dismiss button fires callback`() =
        runComposeUiTest {
            var dismissed = false
            setContent {
                Theme {
                    SquawkInfoDialog(squawk = "7700", onDismiss = { dismissed = true })
                }
            }
            onNodeWithText("Dismiss").performClick()
            assertTrue(dismissed)
        }
}
