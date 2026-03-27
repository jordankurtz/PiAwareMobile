package com.jordankurtz.piawaremobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.ServerType
import com.jordankurtz.piawaremobile.settings.ui.AddServerDialog
import com.jordankurtz.piawaremobile.settings.ui.EditServerDialog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AddServerDialogTest {
    @Test
    fun displaysDialogElements() =
        runComposeUiTest {
            setContent {
                AddServerDialog(onDismiss = {}, onConfirm = { _, _, _ -> })
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
                AddServerDialog(onDismiss = {}, onConfirm = { _, _, _ -> })
            }
            onNodeWithText("Save").assertIsNotEnabled()
        }

    @Test
    fun cancelButtonFiresCallback() =
        runComposeUiTest {
            var dismissed = false
            setContent {
                AddServerDialog(onDismiss = { dismissed = true }, onConfirm = { _, _, _ -> })
            }
            onNodeWithText("Cancel").performClick()
            assertTrue(dismissed)
        }

    @Test
    fun showsValidationErrors() =
        runComposeUiTest {
            setContent {
                AddServerDialog(onDismiss = {}, onConfirm = { _, _, _ -> })
            }
            onNodeWithText("Name is required").assertIsDisplayed()
            onNodeWithText("Address is required").assertIsDisplayed()
        }

    @Test
    fun displaysServerTypeSelector() =
        runComposeUiTest {
            setContent {
                AddServerDialog(onDismiss = {}, onConfirm = { _, _, _ -> })
            }
            onNodeWithText("Server Type").assertIsDisplayed()
            onNodeWithText("PiAware / dump1090").assertIsDisplayed()
            onNodeWithText("Standard local receiver").assertIsDisplayed()
        }

    @Test
    fun defaultServerTypeIsPiaware() =
        runComposeUiTest {
            var savedType: ServerType? = null
            setContent {
                AddServerDialog(
                    onDismiss = {},
                    onConfirm = { _, _, type -> savedType = type },
                )
            }
            onNodeWithText("Name").performClick()
            onNodeWithText("Name").performTextInput("Test")
            onNodeWithText("Address").performClick()
            onNodeWithText("Address").performTextInput("test.local")
            onNodeWithText("Save").performClick()
            assertTrue(savedType == ServerType.PIAWARE)
        }

    @Test
    fun selectingReadsbUpdatesType() =
        runComposeUiTest {
            var savedType: ServerType? = null
            setContent {
                AddServerDialog(
                    onDismiss = {},
                    onConfirm = { _, _, type -> savedType = type },
                )
            }
            // Open the dropdown
            onNodeWithText("PiAware / dump1090").performClick()
            // Select readsb
            onNodeWithText("readsb").performClick()
            // Verify description changed
            onNodeWithText("Alternative decoder with trace API support").assertIsDisplayed()
            // Fill required fields and save
            onNodeWithText("Name").performClick()
            onNodeWithText("Name").performTextInput("Test")
            onNodeWithText("Address").performClick()
            onNodeWithText("Address").performTextInput("test.local")
            onNodeWithText("Save").performClick()
            assertTrue(savedType == ServerType.READSB)
        }

    @Test
    fun editDialogPreSelectsReadsbType() =
        runComposeUiTest {
            val server =
                Server(
                    name = "My Readsb",
                    address = "readsb.local",
                    type = ServerType.READSB,
                )
            setContent {
                EditServerDialog(
                    server = server,
                    onDismiss = {},
                    onConfirm = { _, _, _ -> },
                )
            }
            // The button text should show the current type
            onNodeWithText("readsb").assertIsDisplayed()
            // The description should match readsb
            onNodeWithText("Alternative decoder with trace API support").assertIsDisplayed()
        }

    @Test
    fun editDialogPreSelectsPiawareTypeByDefault() =
        runComposeUiTest {
            val server =
                Server(
                    name = "My PiAware",
                    address = "piaware.local",
                    type = ServerType.PIAWARE,
                )
            setContent {
                EditServerDialog(
                    server = server,
                    onDismiss = {},
                    onConfirm = { _, _, _ -> },
                )
            }
            onNodeWithText("PiAware / dump1090").assertIsDisplayed()
            onNodeWithText("Standard local receiver").assertIsDisplayed()
        }

    @Test
    fun editDialogChangingOnlyTypeSavesCorrectly() =
        runComposeUiTest {
            var savedType: ServerType? = null
            val server =
                Server(
                    name = "Test Server",
                    address = "test.local",
                    type = ServerType.PIAWARE,
                )
            setContent {
                EditServerDialog(
                    server = server,
                    onDismiss = {},
                    onConfirm = { _, _, type -> savedType = type },
                )
            }
            // Change type to readsb
            onNodeWithText("PiAware / dump1090").performClick()
            onNodeWithText("readsb").performClick()
            // Save without changing name or address
            onNodeWithText("Save").performClick()
            assertEquals(ServerType.READSB, savedType)
        }
}
