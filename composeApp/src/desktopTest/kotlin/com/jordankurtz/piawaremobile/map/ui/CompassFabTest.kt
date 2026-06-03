package com.jordankurtz.piawaremobile.map.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertFails
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
    fun compassHiddenWhenBearingIsZero() =
        runComposeUiTest {
            setContent {
                CompassFab(bearing = 0f, onResetNorth = {})
            }
            assertFails {
                onNodeWithContentDescription("Reset north").assertIsDisplayed()
            }
        }

    @Test
    fun compassHiddenWhenBearingWithinOneDegreeTolerance() =
        runComposeUiTest {
            setContent {
                CompassFab(bearing = 0.5f, onResetNorth = {})
            }
            assertFails {
                onNodeWithContentDescription("Reset north").assertIsDisplayed()
            }
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
