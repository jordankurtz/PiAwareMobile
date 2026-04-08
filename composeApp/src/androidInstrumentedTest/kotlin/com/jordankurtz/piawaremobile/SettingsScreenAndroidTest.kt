package com.jordankurtz.piawaremobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.SettingsViewModel
import com.jordankurtz.piawaremobile.settings.ui.MainScreen
import com.jordankurtz.piawaremobile.settings.usecase.SettingsService
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenAndroidTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createViewModel(): SettingsViewModel {
        val settingsService =
            mock<SettingsService> {
                every { loadSettings() } returns flowOf(Async.Success(Settings()))
            }

        return SettingsViewModel(
            settingsService = settingsService,
        )
    }

    @Test
    fun settingsScreenRenders() {
        composeTestRule.setContent {
            MainScreen(onServersClicked = {}, viewModel = createViewModel())
        }
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Map").assertIsDisplayed()
        composeTestRule.onNode(hasText("Servers") and hasClickAction()).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun settingsScreenDisplaysSwitches() {
        composeTestRule.setContent {
            MainScreen(onServersClicked = {}, viewModel = createViewModel())
        }
        composeTestRule.onNodeWithText("Center map on user").assertIsDisplayed()
        composeTestRule.onNodeWithText("Restore map position").assertIsDisplayed()
        composeTestRule.onNodeWithText("Show receiver locations").assertIsDisplayed()
    }

    @Test
    fun serversItemIsClickable() {
        var clicked = false
        composeTestRule.setContent {
            MainScreen(onServersClicked = { clicked = true }, viewModel = createViewModel())
        }
        composeTestRule.onNode(hasText("Servers") and hasClickAction()).performScrollTo().performClick()
        assertTrue(clicked)
    }
}
