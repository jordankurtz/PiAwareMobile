package com.jordankurtz.piawaremobile.map.offline

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
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
}
