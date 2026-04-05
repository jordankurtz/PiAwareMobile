package com.jordankurtz.piawaremobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.map.offline.DownloadStatus
import com.jordankurtz.piawaremobile.map.offline.OfflineRegion
import com.jordankurtz.piawaremobile.settings.ui.OfflineMapsScreen
import kotlin.test.Test
import kotlin.test.assertEquals
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
                        id = 1L,
                        name = "Home Area",
                        minZoom = 8,
                        maxZoom = 14,
                        minLat = 37.2,
                        maxLat = 38.1,
                        minLon = -122.5,
                        maxLon = -121.8,
                        providerId = "osm",
                        createdAt = 1700000000000L,
                        tileCount = 2400L,
                        sizeBytes = 42L * 1024 * 1024,
                    ),
                    OfflineRegion(
                        id = 2L,
                        name = "Airport Region",
                        minZoom = 10,
                        maxZoom = 16,
                        minLat = 37.2,
                        maxLat = 38.1,
                        minLon = -122.5,
                        maxLon = -121.8,
                        providerId = "osm",
                        createdAt = 1700000000000L,
                        tileCount = 5000L,
                        sizeBytes = 128L * 1024 * 1024,
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
                        id = 1L,
                        name = "Home Area",
                        minZoom = 8,
                        maxZoom = 14,
                        minLat = 37.2,
                        maxLat = 38.1,
                        minLon = -122.5,
                        maxLon = -121.8,
                        providerId = "osm",
                        createdAt = 1700000000000L,
                        tileCount = 2400L,
                        sizeBytes = 42L * 1024 * 1024,
                    ),
                    OfflineRegion(
                        id = 2L,
                        name = "Airport Region",
                        minZoom = 10,
                        maxZoom = 16,
                        minLat = 37.2,
                        maxLat = 38.1,
                        minLon = -122.5,
                        maxLon = -121.8,
                        providerId = "osm",
                        createdAt = 1700000000000L,
                        tileCount = 5000L,
                        sizeBytes = 128L * 1024 * 1024,
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
                        id = 1L,
                        name = "Home Area",
                        minZoom = 8,
                        maxZoom = 14,
                        minLat = 37.2,
                        maxLat = 38.1,
                        minLon = -122.5,
                        maxLon = -121.8,
                        providerId = "osm",
                        createdAt = 1700000000000L,
                        tileCount = 2400L,
                        sizeBytes = 42L * 1024 * 1024,
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
                        id = 1L,
                        name = "Home Area",
                        minZoom = 8,
                        maxZoom = 14,
                        minLat = 37.2,
                        maxLat = 38.1,
                        minLon = -122.5,
                        maxLon = -121.8,
                        providerId = "osm",
                        createdAt = 1700000000000L,
                        tileCount = 2400L,
                        sizeBytes = 42L * 1024 * 1024,
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
                    id = 1L,
                    name = "Home Area",
                    minZoom = 8,
                    maxZoom = 14,
                    minLat = 37.2,
                    maxLat = 38.1,
                    minLon = -122.5,
                    maxLon = -121.8,
                    providerId = "osm",
                    createdAt = 1700000000000L,
                    tileCount = 2400L,
                    sizeBytes = 42L * 1024 * 1024,
                )
            setContent {
                OfflineMapsScreen(
                    onBack = {},
                    regions = listOf(region),
                    onDeleteRegion = { deletedRegion = it },
                )
            }
            onNodeWithContentDescription("Delete region").performClick()
            assertEquals(region, deletedRegion)
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

    @Test
    fun downloadingRegionShowsProgressIndicator() =
        runComposeUiTest {
            val region =
                OfflineRegion(
                    id = 1L,
                    name = "My Region",
                    minZoom = 8,
                    maxZoom = 14,
                    minLat = 37.0,
                    maxLat = 38.0,
                    minLon = -122.0,
                    maxLon = -121.0,
                    providerId = "osm",
                    createdAt = 1000L,
                    tileCount = 100L,
                    sizeBytes = 1_500_000L,
                    status = DownloadStatus.DOWNLOADING,
                    downloadedTileCount = 40L,
                )
            setContent {
                OfflineMapsScreen(
                    onBack = {},
                    regions = listOf(region),
                )
            }
            onNodeWithContentDescription("Downloading", substring = true).assertExists()
        }
}
