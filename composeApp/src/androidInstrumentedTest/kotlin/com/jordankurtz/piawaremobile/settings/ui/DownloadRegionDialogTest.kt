package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class DownloadRegionDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun downloadRegionDialogRendersElements() {
        composeTestRule.setContent {
            DownloadRegionDialog(onDismiss = {}, onConfirm = { _, _, _ -> })
        }
        composeTestRule.onNodeWithText("Download Region").assertIsDisplayed()
        composeTestRule.onNodeWithText("Region name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Download").assertIsDisplayed()
    }

    @Test
    fun downloadButtonDisabledWhenNameBlank() {
        composeTestRule.setContent {
            DownloadRegionDialog(onDismiss = {}, onConfirm = { _, _, _ -> })
        }
        composeTestRule.onNodeWithText("Download").assertIsNotEnabled()
    }
}
