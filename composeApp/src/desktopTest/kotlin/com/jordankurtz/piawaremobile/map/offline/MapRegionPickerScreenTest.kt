package com.jordankurtz.piawaremobile.map.offline

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
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
                )
            }
            onNodeWithText("Cancel").performClick()
            assertTrue(dismissed)
        }
}
