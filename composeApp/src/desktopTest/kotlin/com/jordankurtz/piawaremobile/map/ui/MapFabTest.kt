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
class MapFabTest {
    @Test
    fun contentIsRendered() =
        runComposeUiTest {
            setContent {
                MapFab(onClick = {}) { Text("LABEL") }
            }
            onNodeWithText("LABEL").assertIsDisplayed()
        }

    @Test
    fun clickFiresCallback() =
        runComposeUiTest {
            var clicked = false
            setContent {
                MapFab(onClick = { clicked = true }) { Text("LABEL") }
            }
            onNodeWithText("LABEL").performClick()
            assertTrue(clicked)
        }

    @Test
    fun activeStateRendersWithoutError() =
        runComposeUiTest {
            setContent {
                MapFab(onClick = {}, active = true) { Text("ACTIVE") }
            }
            onNodeWithText("ACTIVE").assertIsDisplayed()
        }
}
