package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DownloadRegionDialogTest {
    @Test
    fun displaysDialogElements() =
        runComposeUiTest {
            setContent {
                DownloadRegionDialog(name = "", onNameChange = {}, onDismiss = {}, onConfirm = { _, _ -> })
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
                DownloadRegionDialog(name = "", onNameChange = {}, onDismiss = {}, onConfirm = { _, _ -> })
            }
            onNodeWithText("Download").assertIsNotEnabled()
        }

    @Test
    fun downloadButtonEnabledWhenNameNonBlankAndBoundsSelected() =
        runComposeUiTest {
            val bounds = BoundingBox(minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0)
            setContent {
                DownloadRegionDialog(
                    name = "My Region",
                    onNameChange = {},
                    onDismiss = {},
                    onConfirm = { _, _ -> },
                    selectedBounds = bounds,
                )
            }
            onNodeWithText("Download").assertIsEnabled()
        }

    @Test
    fun downloadButtonDisabledWhenBoundsNull() =
        runComposeUiTest {
            setContent {
                DownloadRegionDialog(
                    name = "My Region",
                    onNameChange = {},
                    onDismiss = {},
                    onConfirm = { _, _ -> },
                    selectedBounds = null,
                )
            }
            onNodeWithText("Download").assertIsNotEnabled()
        }

    @Test
    fun downloadButtonDisabledWhenTileCountExceedsLimit() =
        runComposeUiTest {
            // World bounds at zoom 1-18 → millions of tiles
            val bounds = BoundingBox(minLat = -85.0, maxLat = 85.0, minLon = -180.0, maxLon = 180.0)
            setContent {
                DownloadRegionDialog(
                    name = "World",
                    onNameChange = {},
                    onDismiss = {},
                    onConfirm = { _, _ -> },
                    selectedBounds = bounds,
                )
            }
            onNodeWithText("Download").assertIsNotEnabled()
            onNodeWithText("Too many tiles", substring = true).assertIsDisplayed()
        }

    @Test
    fun typingInNameFieldCallsOnNameChange() =
        runComposeUiTest {
            var capturedName = ""
            setContent {
                var name by remember { mutableStateOf("") }
                DownloadRegionDialog(
                    name = name,
                    onNameChange = {
                        name = it
                        capturedName = it
                    },
                    onDismiss = {},
                    onConfirm = { _, _ -> },
                )
            }
            onNodeWithText("Region name").performClick()
            onNodeWithText("Region name").performTextInput("Airport Area")
            assertTrue(capturedName.isNotBlank())
        }

    @Test
    fun cancelButtonFiresDismissCallback() =
        runComposeUiTest {
            var dismissed = false
            setContent {
                DownloadRegionDialog(name = "", onNameChange = {}, onDismiss = { dismissed = true }, onConfirm = {
                        _,
                        _,
                    ->
                })
            }
            onNodeWithText("Cancel").performClick()
            assertTrue(dismissed)
        }

    @Test
    fun confirmFiresOnConfirmCallback() =
        runComposeUiTest {
            var confirmCalled = false
            val bounds = BoundingBox(minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0)
            setContent {
                DownloadRegionDialog(
                    name = "Airport Area",
                    onNameChange = {},
                    onDismiss = {},
                    onConfirm = { _, _ -> confirmCalled = true },
                    selectedBounds = bounds,
                )
            }
            onNodeWithText("Download").performClick()
            assertTrue(confirmCalled)
        }

    @Test
    fun selectOnMapButtonIsVisible() =
        runComposeUiTest {
            setContent {
                DownloadRegionDialog(name = "", onNameChange = {}, onDismiss = {}, onConfirm = { _, _ -> })
            }
            onNodeWithText("Select on map").assertIsDisplayed()
        }

    @Test
    fun selectOnMapButtonFiresCallback() =
        runComposeUiTest {
            var selectOnMapCalled = false
            setContent {
                DownloadRegionDialog(
                    name = "",
                    onNameChange = {},
                    onDismiss = {},
                    onConfirm = { _, _ -> },
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
                    name = "",
                    onNameChange = {},
                    onDismiss = {},
                    onConfirm = { _, _ -> },
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
                    name = "",
                    onNameChange = {},
                    onDismiss = {},
                    onConfirm = { _, _ -> },
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
                    name = "",
                    onNameChange = {},
                    onDismiss = {},
                    onConfirm = { _, _ -> },
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
                    name = "",
                    onNameChange = {},
                    onDismiss = {},
                    onConfirm = { _, _ -> },
                    selectedBounds = null,
                )
            }
            onNodeWithText("Select a region on the map to see an estimate").assertExists()
        }
}
