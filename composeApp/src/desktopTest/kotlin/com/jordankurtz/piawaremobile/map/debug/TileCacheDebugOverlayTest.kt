package com.jordankurtz.piawaremobile.map.debug

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TileCacheDebugOverlayTest {
    @Test
    fun displaysDiskHitsAndNetworkFetches() =
        runComposeUiTest {
            setContent {
                TileCacheDebugOverlay(
                    stats = TileCacheStats(diskHits = 42L, networkFetches = 8L),
                )
            }
            onNodeWithText("Tiles  D:42  O:0  N:8  84% cache", substring = false)
                .assertIsDisplayed()
        }

    @Test
    fun displaysHitPercentage() =
        runComposeUiTest {
            setContent {
                TileCacheDebugOverlay(
                    stats = TileCacheStats(diskHits = 3L, networkFetches = 7L),
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
                    stats = TileCacheStats(diskHits = 5L, networkFetches = 3L, errors = 2L),
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
                    stats = TileCacheStats(diskHits = 5L, networkFetches = 3L, errors = 0L),
                )
            }
            onNodeWithText("Tiles  D:5  O:0  N:3  62% cache", substring = false)
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
            onNodeWithText("Tiles  D:0  O:0  N:0  0% cache", substring = false)
                .assertIsDisplayed()
        }

    @Test
    fun displaysOfflineHits() =
        runComposeUiTest {
            setContent {
                TileCacheDebugOverlay(
                    stats = TileCacheStats(diskHits = 10L, offlineHits = 5L, networkFetches = 3L),
                )
            }
            onNodeWithText("O:5", substring = true).assertIsDisplayed()
        }
}
