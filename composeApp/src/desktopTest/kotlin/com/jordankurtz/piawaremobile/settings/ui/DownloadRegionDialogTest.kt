package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.map.offline.BoundingBox
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
            onNodeWithText("Select a region on the map to see an estimate").assertIsDisplayed()
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

    @Test
    fun selectOnMapButtonIsVisible() =
        runComposeUiTest {
            setContent {
                DownloadRegionDialog(onDismiss = {}, onConfirm = { _, _, _ -> })
            }
            onNodeWithText("Select on map").assertIsDisplayed()
        }

    @Test
    fun selectOnMapButtonFiresCallback() =
        runComposeUiTest {
            var selectOnMapCalled = false
            setContent {
                DownloadRegionDialog(
                    onDismiss = {},
                    onConfirm = { _, _, _ -> },
                    onSelectOnMap = { selectOnMapCalled = true },
                )
            }
            onNodeWithText("Select on map").performClick()
            assertTrue(selectOnMapCalled)
        }

    @Test
    fun boundsTextShownWhenSelectedBoundsNonNull() =
        runComposeUiTest {
            val bounds = BoundingBox(minLat = 37.0, maxLat = 38.0, minLon = -122.5, maxLon = -121.5)
            setContent {
                DownloadRegionDialog(
                    onDismiss = {},
                    onConfirm = { _, _, _ -> },
                    selectedBounds = bounds,
                )
            }
            onNodeWithText("Selected bounds: 37.0000, -122.5000 – 38.0000, -121.5000").assertIsDisplayed()
        }

    @Test
    fun boundsTextNotShownWhenSelectedBoundsNull() =
        runComposeUiTest {
            setContent {
                DownloadRegionDialog(
                    onDismiss = {},
                    onConfirm = { _, _, _ -> },
                    selectedBounds = null,
                )
            }
            onNodeWithText("Selected bounds", substring = true).assertDoesNotExist()
        }

    @Test
    fun estimateShowsTileCountAndMbWhenBoundsAreSet() =
        runComposeUiTest {
            val bounds = BoundingBox(minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0)
            setContent {
                DownloadRegionDialog(
                    onDismiss = {},
                    onConfirm = { _, _, _ -> },
                    selectedBounds = bounds,
                )
            }
            onNodeWithText("tiles", substring = true).assertExists()
        }

    @Test
    fun estimateShowsNoBoundsMessageWhenBoundsNull() =
        runComposeUiTest {
            setContent {
                DownloadRegionDialog(
                    onDismiss = {},
                    onConfirm = { _, _, _ -> },
                    selectedBounds = null,
                )
            }
            onNodeWithText("Select a region on the map to see an estimate").assertExists()
        }
}
