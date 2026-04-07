package com.jordankurtz.piawaremobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
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
            val scrollable = onNode(hasScrollAction())
            scrollable.performScrollToNode(hasText("Show receiver locations"))
            onNodeWithText("Show receiver locations").assertIsDisplayed()
            scrollable.performScrollToNode(hasText("Show User Location on Map"))
            onNodeWithText("Show User Location on Map").assertIsDisplayed()
            scrollable.performScrollToNode(hasText("Show Minimap Trails"))
            onNodeWithText("Show Minimap Trails").assertIsDisplayed()
            scrollable.performScrollToNode(hasText("Center map on user"))
            onNodeWithText("Center map on user").assertIsDisplayed()
            scrollable.performScrollToNode(hasText("Restore map position"))
            onNodeWithText("Restore map position").assertIsDisplayed()
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
            onNode(hasScrollAction()).performScrollToNode(hasText("Enable FlightAware API"))
            onNodeWithText("Enable FlightAware API").assertIsDisplayed()
        }

    @Test
    fun displaysOfflineMapsItem() =
        runComposeUiTest {
            setContent {
                MainScreen(onServersClicked = {}, viewModel = createViewModel())
            }
            onNodeWithText("Offline Maps").assertIsDisplayed()
        }

    @Test
    fun offlineMapsItemFiresCallback() =
        runComposeUiTest {
            var clicked = false
            setContent {
                MainScreen(
                    onServersClicked = {},
                    onOfflineMapsClicked = { clicked = true },
                    viewModel = createViewModel(),
                )
            }
            onNodeWithText("Offline Maps").performClick()
            assertTrue(clicked)
        }
}
