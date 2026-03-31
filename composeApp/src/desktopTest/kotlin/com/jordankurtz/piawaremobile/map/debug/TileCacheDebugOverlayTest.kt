package com.jordankurtz.piawaremobile.map.debug

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TileCacheDebugOverlayTest {
    @Test
    fun displaysDiscHitsAndNetworkFetches() =
        runComposeUiTest {
            setContent {
                TileCacheDebugOverlay(
                    stats = TileCacheStats(diskHits = 42, networkFetches = 8),
                )
            }
            onNodeWithText("Tiles  D:42  N:8  84% cache", substring = false)
                .assertIsDisplayed()
        }

    @Test
    fun displaysHitPercentage() =
        runComposeUiTest {
            setContent {
                TileCacheDebugOverlay(
                    stats = TileCacheStats(diskHits = 3, networkFetches = 7),
                )
            }
            onNodeWithText("30% cache", substring = true)
                .assertIsDisplayed()
        }

    @Test
    fun showsErrorCountWhenErrorsGreaterThanZero() =
        runComposeUiTest {
            setContent {
                TileCacheDebugOverlay(
                    stats = TileCacheStats(diskHits = 5, networkFetches = 3, errors = 2),
                )
            }
            onNodeWithText("E:2", substring = true)
                .assertIsDisplayed()
        }

    @Test
    fun hidesErrorCountWhenErrorsIsZero() =
        runComposeUiTest {
            setContent {
                TileCacheDebugOverlay(
                    stats = TileCacheStats(diskHits = 5, networkFetches = 3, errors = 0),
                )
            }
            onNodeWithText("Tiles  D:5  N:3  62% cache", substring = false)
                .assertIsDisplayed()
        }

    @Test
    fun displaysZeroPercentWhenNoRequests() =
        runComposeUiTest {
            setContent {
                TileCacheDebugOverlay(
                    stats = TileCacheStats(),
                )
            }
            onNodeWithText("Tiles  D:0  N:0  0% cache", substring = false)
                .assertIsDisplayed()
        }
}
