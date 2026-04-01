package com.jordankurtz.piawaremobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.jordankurtz.piawaremobile.map.offline.OfflineRegion
import com.jordankurtz.piawaremobile.settings.ui.OfflineMapsScreen
import org.junit.Rule
import org.junit.Test

class OfflineMapsScreenAndroidTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun offlineMapsScreenRendersEmptyState() {
        composeTestRule.setContent {
            OfflineMapsScreen(
                onBack = {},
                regions = emptyList(),
            )
        }
        composeTestRule.onNodeWithText("Offline Maps").assertIsDisplayed()
        composeTestRule.onNodeWithText("No offline regions").assertIsDisplayed()
    }

    @Test
    fun offlineMapsScreenRendersWithRegions() {
        val regions =
            listOf(
                OfflineRegion(
                    id = "1",
                    name = "Test Region",
                    minZoom = 8,
                    maxZoom = 14,
                    storageSizeMb = 50,
                    downloadDate = "2026-03-01",
                ),
            )
        composeTestRule.setContent {
            OfflineMapsScreen(
                onBack = {},
                regions = regions,
            )
        }
        composeTestRule.onNodeWithText("Test Region").assertIsDisplayed()
        composeTestRule.onNodeWithText("50 MB").assertIsDisplayed()
    }
}
