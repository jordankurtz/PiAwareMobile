package com.jordankurtz.piawaremobile.map.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CompassFabTest {
    @Test
    fun compassVisibleWhenBearingNonZero() =
        runComposeUiTest {
            setContent {
                CompassFab(bearing = 45f, onResetNorth = {})
            }
            onNodeWithContentDescription("Reset north").assertIsDisplayed()
        }

    @Test
    fun compassAlwaysDisplayedWhenBearingIsZero() =
        runComposeUiTest {
            setContent {
                CompassFab(bearing = 0f, onResetNorth = {})
            }
            onNodeWithContentDescription("Reset north").assertIsDisplayed()
        }

    @Test
    fun tapTriggersResetNorth() =
        runComposeUiTest {
            var resetCalled = false
            setContent {
                CompassFab(bearing = 90f, onResetNorth = { resetCalled = true })
            }
            onNodeWithContentDescription("Reset north").performClick()
            assertTrue(resetCalled)
        }
}
