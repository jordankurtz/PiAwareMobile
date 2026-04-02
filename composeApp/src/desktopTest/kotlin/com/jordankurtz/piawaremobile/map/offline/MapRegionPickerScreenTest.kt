package com.jordankurtz.piawaremobile.map.offline

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MapRegionPickerScreenTest {
    @Test
    fun confirmAndCancelButtonsRender() =
        runComposeUiTest {
            setContent {
                MapRegionPickerContent(
                    onRegionSelected = {},
                    onDismiss = {},
                    mapLayer = {},
                )
            }
            onNodeWithText("Confirm").assertIsDisplayed()
            onNodeWithText("Cancel").assertIsDisplayed()
        }

    @Test
    fun cancelTriggersDismissCallback() =
        runComposeUiTest {
            var dismissed = false
            setContent {
                MapRegionPickerContent(
                    onRegionSelected = {},
                    onDismiss = { dismissed = true },
                    mapLayer = {},
                )
            }
            onNodeWithText("Cancel").performClick()
            assertTrue(dismissed)
        }

    @Test
    fun confirmTriggersOnRegionSelectedCallback() =
        runComposeUiTest {
            var selectedBounds: BoundingBox? = null
            setContent {
                MapRegionPickerContent(
                    onRegionSelected = { selectedBounds = it },
                    onDismiss = {},
                    mapLayer = {},
                )
            }
            onNodeWithText("Confirm").performClick()
            assertNotNull(selectedBounds)
        }

    @Test
    fun modeToggleButtonRendersInBoxModeByDefault() =
        runComposeUiTest {
            setContent {
                MapRegionPickerContent(
                    onRegionSelected = {},
                    onDismiss = {},
                    mapLayer = {},
                )
            }
            // Default mode is BOX — button should offer switching to map mode
            onNodeWithContentDescription("Switch to map mode").assertIsDisplayed()
            onNodeWithText("Editing region").assertIsDisplayed()
        }

    @Test
    fun modeToggleButtonSwitchesToMapMode() =
        runComposeUiTest {
            setContent {
                MapRegionPickerContent(
                    onRegionSelected = {},
                    onDismiss = {},
                    mapLayer = {},
                )
            }
            // Click toggle: BOX -> MAP
            onNodeWithContentDescription("Switch to map mode").performClick()
            onNodeWithContentDescription("Switch to box mode").assertIsDisplayed()
            onNodeWithText("Moving map").assertIsDisplayed()
        }

    @Test
    fun modeToggleButtonRoundTrips() =
        runComposeUiTest {
            setContent {
                MapRegionPickerContent(
                    onRegionSelected = {},
                    onDismiss = {},
                    mapLayer = {},
                )
            }
            // BOX -> MAP -> BOX
            onNodeWithContentDescription("Switch to map mode").performClick()
            onNodeWithContentDescription("Switch to box mode").performClick()
            onNodeWithContentDescription("Switch to map mode").assertIsDisplayed()
            onNodeWithText("Editing region").assertIsDisplayed()
        }
}
