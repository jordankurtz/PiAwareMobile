package com.jordankurtz.piawaremobile.map.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MapFabAndroidTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun contentIsRendered() {
        composeTestRule.setContent {
            MapFab(onClick = {}) { Text("LABEL") }
        }
        composeTestRule.onNodeWithText("LABEL").assertIsDisplayed()
    }

    @Test
    fun clickFiresCallback() {
        var clicked = false
        composeTestRule.setContent {
            MapFab(onClick = { clicked = true }) { Text("LABEL") }
        }
        composeTestRule.onNodeWithText("LABEL").performClick()
        assertTrue(clicked)
    }

    @Test
    fun activeStateRendersWithoutError() {
        composeTestRule.setContent {
            MapFab(onClick = {}, active = true) { Text("ACTIVE") }
        }
        composeTestRule.onNodeWithText("ACTIVE").assertIsDisplayed()
    }

    @Test
    fun compassFabVisibleWhenBearingNonZero() {
        composeTestRule.setContent {
            CompassFab(bearing = 45f, onResetNorth = {})
        }
        composeTestRule.onNodeWithContentDescription("Reset north").assertIsDisplayed()
    }

    @Test
    fun compassFabHiddenWhenBearingIsZero() {
        composeTestRule.setContent {
            CompassFab(bearing = 0f, onResetNorth = {})
        }
        composeTestRule.onNodeWithContentDescription("Reset north").assertDoesNotExist()
    }

    @Test
    fun compassFabTapTriggersResetNorth() {
        var resetCalled = false
        composeTestRule.setContent {
            CompassFab(bearing = 90f, onResetNorth = { resetCalled = true })
        }
        composeTestRule.onNodeWithContentDescription("Reset north").performClick()
        assertTrue(resetCalled)
    }
}
