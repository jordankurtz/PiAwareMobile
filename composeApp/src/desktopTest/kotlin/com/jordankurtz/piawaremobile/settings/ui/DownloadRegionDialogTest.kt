package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DownloadRegionDialogTest {
    @Test
    fun displaysDialogElements() =
        runComposeUiTest {
            setContent {
                DownloadRegionDialog(onDismiss = {}, onConfirm = { _, _, _ -> })
            }
            onNodeWithText("Download Region").assertIsDisplayed()
            onNodeWithText("Region name").assertIsDisplayed()
            onNodeWithText("Min zoom: 8").assertIsDisplayed()
            onNodeWithText("Max zoom: 14").assertIsDisplayed()
            onNodeWithText("Estimated size depends on zoom levels and area").assertIsDisplayed()
            onNodeWithText("Cancel").assertIsDisplayed()
            onNodeWithText("Download").assertIsDisplayed()
        }

    @Test
    fun downloadButtonDisabledWhenNameBlank() =
        runComposeUiTest {
            setContent {
                DownloadRegionDialog(onDismiss = {}, onConfirm = { _, _, _ -> })
            }
            onNodeWithText("Download").assertIsNotEnabled()
        }

    @Test
    fun downloadButtonEnabledWhenNameNonBlank() =
        runComposeUiTest {
            setContent {
                DownloadRegionDialog(onDismiss = {}, onConfirm = { _, _, _ -> })
            }
            onNodeWithText("Region name").performClick()
            onNodeWithText("Region name").performTextInput("My Region")
            onNodeWithText("Download").assertIsEnabled()
        }

    @Test
    fun cancelButtonFiresDismissCallback() =
        runComposeUiTest {
            var dismissed = false
            setContent {
                DownloadRegionDialog(onDismiss = { dismissed = true }, onConfirm = { _, _, _ -> })
            }
            onNodeWithText("Cancel").performClick()
            assertTrue(dismissed)
        }

    @Test
    fun confirmWithNameCallsOnConfirmWithCorrectName() =
        runComposeUiTest {
            var confirmedName: String? = null
            setContent {
                DownloadRegionDialog(
                    onDismiss = {},
                    onConfirm = { name, _, _ -> confirmedName = name },
                )
            }
            onNodeWithText("Region name").performClick()
            onNodeWithText("Region name").performTextInput("Airport Area")
            onNodeWithText("Download").performClick()
            assertEquals("Airport Area", confirmedName)
        }
}
