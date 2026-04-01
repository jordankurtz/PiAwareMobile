package com.jordankurtz.piawaremobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.map.offline.OfflineRegion
import com.jordankurtz.piawaremobile.settings.ui.OfflineMapsScreen
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OfflineMapsScreenTest {
    @Test
    fun emptyStateShowsNoRegionsMessage() =
        runComposeUiTest {
            setContent {
                OfflineMapsScreen(
                    onBack = {},
                    regions = emptyList(),
                )
            }
            onNodeWithText("No offline regions").assertIsDisplayed()
            onNodeWithText("Tap + to download a map region for offline use").assertIsDisplayed()
        }

    @Test
    fun displaysOfflineMapsTitle() =
        runComposeUiTest {
            setContent {
                OfflineMapsScreen(
                    onBack = {},
                    regions = emptyList(),
                )
            }
            onNodeWithText("Offline Maps").assertIsDisplayed()
        }

    @Test
    fun populatedStateShowsRegionNames() =
        runComposeUiTest {
            val regions =
                listOf(
                    OfflineRegion(
                        id = "1",
                        name = "Home Area",
                        minZoom = 8,
                        maxZoom = 14,
                        storageSizeMb = 42,
                        downloadDate = "2026-03-01",
                    ),
                    OfflineRegion(
                        id = "2",
                        name = "Airport Region",
                        minZoom = 10,
                        maxZoom = 16,
                        storageSizeMb = 128,
                        downloadDate = "2026-03-15",
                    ),
                )
            setContent {
                OfflineMapsScreen(
                    onBack = {},
                    regions = regions,
                )
            }
            onNodeWithText("Home Area").assertIsDisplayed()
            onNodeWithText("Airport Region").assertIsDisplayed()
        }

    @Test
    fun populatedStateShowsStorageSizes() =
        runComposeUiTest {
            val regions =
                listOf(
                    OfflineRegion(
                        id = "1",
                        name = "Home Area",
                        minZoom = 8,
                        maxZoom = 14,
                        storageSizeMb = 42,
                        downloadDate = "2026-03-01",
                    ),
                    OfflineRegion(
                        id = "2",
                        name = "Airport Region",
                        minZoom = 10,
                        maxZoom = 16,
                        storageSizeMb = 128,
                        downloadDate = "2026-03-15",
                    ),
                )
            setContent {
                OfflineMapsScreen(
                    onBack = {},
                    regions = regions,
                )
            }
            onNodeWithText("42 MB").assertIsDisplayed()
            onNodeWithText("128 MB").assertIsDisplayed()
        }

    @Test
    fun populatedStateShowsZoomRanges() =
        runComposeUiTest {
            val regions =
                listOf(
                    OfflineRegion(
                        id = "1",
                        name = "Home Area",
                        minZoom = 8,
                        maxZoom = 14,
                        storageSizeMb = 42,
                        downloadDate = "2026-03-01",
                    ),
                )
            setContent {
                OfflineMapsScreen(
                    onBack = {},
                    regions = regions,
                )
            }
            onNodeWithText("Zoom 8 – 14").assertIsDisplayed()
        }

    @Test
    fun addRegionButtonIsDisplayed() =
        runComposeUiTest {
            setContent {
                OfflineMapsScreen(
                    onBack = {},
                    regions = emptyList(),
                )
            }
            onNodeWithContentDescription("Add region").assertIsDisplayed()
        }

    @Test
    fun deleteButtonIsRenderedForRegion() =
        runComposeUiTest {
            val regions =
                listOf(
                    OfflineRegion(
                        id = "1",
                        name = "Home Area",
                        minZoom = 8,
                        maxZoom = 14,
                        storageSizeMb = 42,
                        downloadDate = "2026-03-01",
                    ),
                )
            setContent {
                OfflineMapsScreen(
                    onBack = {},
                    regions = regions,
                )
            }
            onNodeWithContentDescription("Delete region").assertIsDisplayed()
        }

    @Test
    fun deleteButtonInvokesOnDeleteRegion() =
        runComposeUiTest {
            var deletedRegion: OfflineRegion? = null
            val region =
                OfflineRegion(
                    id = "1",
                    name = "Home Area",
                    minZoom = 8,
                    maxZoom = 14,
                    storageSizeMb = 42,
                    downloadDate = "2026-03-01",
                )
            setContent {
                OfflineMapsScreen(
                    onBack = {},
                    regions = listOf(region),
                    onDeleteRegion = { deletedRegion = it },
                )
            }
            onNodeWithContentDescription("Delete region").performClick()
            assertTrue(deletedRegion == region)
        }

    @Test
    fun backButtonInvokesOnBack() =
        runComposeUiTest {
            var backClicked = false
            setContent {
                OfflineMapsScreen(
                    onBack = { backClicked = true },
                    regions = emptyList(),
                )
            }
            onNodeWithContentDescription("Back").performClick()
            assertTrue(backClicked)
        }
}
