package com.jordankurtz.piawaremobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.settings.ui.AddServerDialog
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AddServerDialogTest {
    @Test
    fun displaysDialogElements() =
        runComposeUiTest {
            setContent {
                AddServerDialog(onDismiss = {}, onConfirm = { _, _ -> })
            }
            onNodeWithText("Add Server").assertIsDisplayed()
            onNodeWithText("Name").assertIsDisplayed()
            onNodeWithText("Address").assertIsDisplayed()
            onNodeWithText("Cancel").assertIsDisplayed()
            onNodeWithText("Save").assertIsDisplayed()
        }

    @Test
    fun saveButtonDisabledWhenFieldsEmpty() =
        runComposeUiTest {
            setContent {
                AddServerDialog(onDismiss = {}, onConfirm = { _, _ -> })
            }
            onNodeWithText("Save").assertIsNotEnabled()
        }

    @Test
    fun cancelButtonFiresCallback() =
        runComposeUiTest {
            var dismissed = false
            setContent {
                AddServerDialog(onDismiss = { dismissed = true }, onConfirm = { _, _ -> })
            }
            onNodeWithText("Cancel").performClick()
            assertTrue(dismissed)
        }

    @Test
    fun showsValidationErrors() =
        runComposeUiTest {
            setContent {
                AddServerDialog(onDismiss = {}, onConfirm = { _, _ -> })
            }
            onNodeWithText("Name is required").assertIsDisplayed()
            onNodeWithText("Address is required").assertIsDisplayed()
        }
}
