package com.jordankurtz.piawaremobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.SettingsViewModel
import com.jordankurtz.piawaremobile.settings.ui.MainScreen
import com.jordankurtz.piawaremobile.settings.usecase.SettingsService
import com.jordankurtz.piawaremobile.testutil.mockSettings
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {
    private fun createViewModel(settings: Settings = mockSettings()): SettingsViewModel {
        val settingsService =
            mock<SettingsService> {
                every { loadSettings() } returns flowOf(Async.Success(settings))
            }
        return SettingsViewModel(settingsService)
    }

    @Test
    fun displaysSettingsTitle() =
        runComposeUiTest {
            setContent {
                MainScreen(onServersClicked = {}, viewModel = createViewModel())
            }
            onNodeWithText("Settings").assertIsDisplayed()
        }

    @Test
    fun displaysServersItem() =
        runComposeUiTest {
            setContent {
                MainScreen(onServersClicked = {}, viewModel = createViewModel())
            }
            onNodeWithText("Servers").assertIsDisplayed()
        }

    @Test
    fun serversItemFiresCallback() =
        runComposeUiTest {
            var clicked = false
            setContent {
                MainScreen(onServersClicked = { clicked = true }, viewModel = createViewModel())
            }
            onNodeWithText("Servers").performClick()
            assertTrue(clicked)
        }

    @Test
    fun displaysSettingsSwitches() =
        runComposeUiTest {
            setContent {
                MainScreen(onServersClicked = {}, viewModel = createViewModel())
            }
            onNodeWithText("Center map on user").assertIsDisplayed()
            onNodeWithText("Restore map position").assertIsDisplayed()
            onNodeWithTag("settings_list").performScrollToNode(hasText("Show receiver locations"))
            onNodeWithText("Show receiver locations").assertIsDisplayed()
            onNodeWithTag("settings_list").performScrollToNode(hasText("Show User Location on Map"))
            onNodeWithText("Show User Location on Map").assertIsDisplayed()
            onNodeWithTag("settings_list").performScrollToNode(hasText("Show Minimap Trails"))
            onNodeWithText("Show Minimap Trails").assertIsDisplayed()
        }

    @Test
    fun displaysPreferencesSection() =
        runComposeUiTest {
            setContent {
                MainScreen(onServersClicked = {}, viewModel = createViewModel())
            }
            onNodeWithText("Preferences").assertIsDisplayed()
        }

    @Test
    fun displaysRefreshIntervalField() =
        runComposeUiTest {
            setContent {
                MainScreen(onServersClicked = {}, viewModel = createViewModel())
            }
            onNodeWithText("Refresh Interval").assertIsDisplayed()
        }

    @Test
    fun displaysFlightAwareSettings() =
        runComposeUiTest {
            setContent {
                MainScreen(onServersClicked = {}, viewModel = createViewModel())
            }
            onNodeWithTag("settings_list").performScrollToNode(hasText("Enable FlightAware API"))
            onNodeWithText("Enable FlightAware API").assertIsDisplayed()
        }

    @Test
    fun displaysZoomSettings() =
        runComposeUiTest {
            setContent {
                MainScreen(onServersClicked = {}, viewModel = createViewModel())
            }
            onNodeWithTag("settings_list").performScrollToNode(hasText("Default Zoom Level"))
            onNodeWithText("Default Zoom Level").assertIsDisplayed()
            onNodeWithTag("settings_list").performScrollToNode(hasText("Min Zoom Level"))
            onNodeWithText("Min Zoom Level").assertIsDisplayed()
            onNodeWithTag("settings_list").performScrollToNode(hasText("Max Zoom Level"))
            onNodeWithText("Max Zoom Level").assertIsDisplayed()
        }
}
