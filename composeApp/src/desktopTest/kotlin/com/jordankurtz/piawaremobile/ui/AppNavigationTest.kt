package com.jordankurtz.piawaremobile.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.BottomNavigationBar
import com.jordankurtz.piawaremobile.model.Screen
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AppNavigationTest {
    @Test
    fun bottomNavDisplaysAllTabs() =
        runComposeUiTest {
            setContent {
                BottomNavigationBar(
                    currentScreen = Screen.Map,
                    onScreenSelected = {},
                )
            }
            onNodeWithText("Map").assertIsDisplayed()
            onNodeWithText("List").assertIsDisplayed()
            onNodeWithText("Settings").assertIsDisplayed()
        }

    @Test
    fun mapTabIsSelectedByDefault() =
        runComposeUiTest {
            setContent {
                BottomNavigationBar(
                    currentScreen = Screen.Map,
                    onScreenSelected = {},
                )
            }
            onNodeWithText("Map").assertIsSelected()
        }

    @Test
    fun clickingTabCallsCallback() =
        runComposeUiTest {
            var selectedScreen: Screen? = null
            setContent {
                BottomNavigationBar(
                    currentScreen = Screen.Map,
                    onScreenSelected = { selectedScreen = it },
                )
            }
            onNodeWithText("Settings").performClick()
            assertEquals(Screen.Settings, selectedScreen)
        }

    @Test
    fun clickingListTabCallsCallback() =
        runComposeUiTest {
            var selectedScreen: Screen? = null
            setContent {
                BottomNavigationBar(
                    currentScreen = Screen.Map,
                    onScreenSelected = { selectedScreen = it },
                )
            }
            onNodeWithText("List").performClick()
            assertEquals(Screen.List, selectedScreen)
        }

    @Test
    fun settingsTabShowsAsSelected() =
        runComposeUiTest {
            setContent {
                BottomNavigationBar(
                    currentScreen = Screen.Settings,
                    onScreenSelected = {},
                )
            }
            onNodeWithText("Settings").assertIsSelected()
        }
}
