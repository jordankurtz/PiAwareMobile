package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.map.offline.DownloadStatus
import com.jordankurtz.piawaremobile.map.offline.OfflineRegion
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OfflineRegionDetailScreenTest {
    private val testRegion =
        OfflineRegion(
            id = 1L,
            name = "Portland Downtown",
            minZoom = 8,
            maxZoom = 14,
            minLat = 45.4,
            maxLat = 45.6,
            minLon = -122.7,
            maxLon = -122.5,
            providerId = "openstreetmap",
            createdAt = 1700000000000L,
            tileCount = 1234L,
            sizeBytes = 18_874_368L,
            status = DownloadStatus.COMPLETE,
        )

    @Test
    fun regionNameShownInTopBar() =
        runComposeUiTest {
            setContent {
                OfflineRegionDetailContent(region = testRegion, mapLayer = {}, onBack = {})
            }
            onNodeWithText("Portland Downtown").assertIsDisplayed()
        }

    @Test
    fun zoomRangeShown() =
        runComposeUiTest {
            setContent {
                OfflineRegionDetailContent(region = testRegion, mapLayer = {}, onBack = {})
            }
            onNodeWithText("8 – 14").assertIsDisplayed()
        }

    @Test
    fun tileCountShown() =
        runComposeUiTest {
            setContent {
                OfflineRegionDetailContent(region = testRegion, mapLayer = {}, onBack = {})
            }
            onNodeWithText("1234").assertIsDisplayed()
        }

    @Test
    fun sizeShown() =
        runComposeUiTest {
            setContent {
                OfflineRegionDetailContent(region = testRegion, mapLayer = {}, onBack = {})
            }
            onNodeWithText("18 MB").assertIsDisplayed()
        }

    @Test
    fun providerShown() =
        runComposeUiTest {
            setContent {
                OfflineRegionDetailContent(region = testRegion, mapLayer = {}, onBack = {})
            }
            onNodeWithText("openstreetmap").assertIsDisplayed()
        }

    @Test
    fun createdLabelShown() =
        runComposeUiTest {
            setContent {
                OfflineRegionDetailContent(region = testRegion, mapLayer = {}, onBack = {})
            }
            onNodeWithText("Created").assertIsDisplayed()
        }

    @Test
    fun backButtonInvokesCallback() =
        runComposeUiTest {
            var clicked = false
            setContent {
                OfflineRegionDetailContent(region = testRegion, mapLayer = {}, onBack = { clicked = true })
            }
            onNodeWithContentDescription("Back").performClick()
            assertTrue(clicked)
        }
}
