package com.jordankurtz.piawaremobile.map.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OverlayFabTest {
    @Test
    fun faaChartsFabRendersInactive() =
        runComposeUiTest {
            setContent {
                MapFab(onClick = {}, active = false) { Text("FAA") }
            }
            onNodeWithText("FAA").assertIsDisplayed()
        }

    @Test
    fun faaChartsFabRendersActive() =
        runComposeUiTest {
            setContent {
                MapFab(onClick = {}, active = true) { Text("FAA") }
            }
            onNodeWithText("FAA").assertIsDisplayed()
        }

    @Test
    fun faaChartsFabClickFires() =
        runComposeUiTest {
            var clicked = false
            setContent {
                MapFab(onClick = { clicked = true }, active = false) { Text("FAA") }
            }
            onNodeWithText("FAA").performClick()
            assertTrue(clicked)
        }

    @Test
    fun airspaceFabRendersInactive() =
        runComposeUiTest {
            setContent {
                MapFab(onClick = {}, active = false) { Text("AIR") }
            }
            onNodeWithText("AIR").assertIsDisplayed()
        }

    @Test
    fun airspaceFabRendersActive() =
        runComposeUiTest {
            setContent {
                MapFab(onClick = {}, active = true) { Text("AIR") }
            }
            onNodeWithText("AIR").assertIsDisplayed()
        }

    @Test
    fun airspaceFabClickFires() =
        runComposeUiTest {
            var clicked = false
            setContent {
                MapFab(onClick = { clicked = true }, active = false) { Text("AIR") }
            }
            onNodeWithText("AIR").performClick()
            assertTrue(clicked)
        }
}
