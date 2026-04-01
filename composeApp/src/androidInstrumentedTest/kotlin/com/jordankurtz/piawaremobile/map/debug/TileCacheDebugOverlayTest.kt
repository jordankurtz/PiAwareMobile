package com.jordankurtz.piawaremobile.map.debug

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TileCacheDebugOverlayTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun overlayRendersWithTileStats() {
        val stats = TileCacheStats(diskHits = 10L, networkFetches = 5L, errors = 1L)
        composeTestRule.setContent {
            TileCacheDebugOverlay(stats = stats)
        }
        composeTestRule.onNodeWithText("D:10", substring = true).assertExists()
        composeTestRule.onNodeWithText("N:5", substring = true).assertExists()
    }
}
