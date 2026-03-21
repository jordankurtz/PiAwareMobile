package com.jordankurtz.piawaremobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jordankurtz.piawaremobile.model.Screen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NavigationAndroidTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottomNavDisplaysAllTabs() {
        composeTestRule.setContent {
            BottomNavigationBar(
                currentScreen = Screen.Map,
                onScreenSelected = {},
            )
        }
        composeTestRule.onNodeWithText("Map").assertIsDisplayed()
        composeTestRule.onNodeWithText("List").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun mapTabIsSelectedByDefault() {
        composeTestRule.setContent {
            BottomNavigationBar(
                currentScreen = Screen.Map,
                onScreenSelected = {},
            )
        }
        composeTestRule.onNodeWithText("Map").assertIsSelected()
    }

    @Test
    fun tabSwitchingWorks() {
        var selectedScreen: Screen? = null
        composeTestRule.setContent {
            BottomNavigationBar(
                currentScreen = Screen.Map,
                onScreenSelected = { selectedScreen = it },
            )
        }
        composeTestRule.onNodeWithText("Settings").performClick()
        assertTrue(selectedScreen == Screen.Settings)
    }
}
