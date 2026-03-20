package com.jordankurtz.piawaremobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.SettingsViewModel
import com.jordankurtz.piawaremobile.settings.ui.MainScreen
import com.jordankurtz.piawaremobile.settings.usecase.AddServerUseCase
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetCenterMapOnUserOnStartUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetEnableFlightAwareApiUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetFlightAwareApiKeyUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetOpenUrlsExternallyUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetRefreshIntervalUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetRestoreMapStateOnStartUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetShowMinimapTrailsUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetShowReceiverLocationsUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetShowUserLocationOnMapUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetTrailDisplayModeUseCase
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
        val loadSettingsUseCase =
            mock<LoadSettingsUseCase> {
                every { invoke() } returns flowOf(Async.Success(settings))
            }

        return SettingsViewModel(
            loadSettingsUseCase = loadSettingsUseCase,
            addServerUseCase = mock<AddServerUseCase>(),
            setRefreshIntervalUseCase = mock<SetRefreshIntervalUseCase>(),
            setCenterMapOnUserOnStartUseCase = mock<SetCenterMapOnUserOnStartUseCase>(),
            setRestoreMapStateOnStartUseCase = mock<SetRestoreMapStateOnStartUseCase>(),
            setShowReceiverLocationsUseCase = mock<SetShowReceiverLocationsUseCase>(),
            setShowUserLocationOnMapUseCase = mock<SetShowUserLocationOnMapUseCase>(),
            setTrailDisplayModeUseCase = mock<SetTrailDisplayModeUseCase>(),
            setShowMinimapTrailsUseCase = mock<SetShowMinimapTrailsUseCase>(),
            setOpenUrlsExternallyUseCase = mock<SetOpenUrlsExternallyUseCase>(),
            setEnableFlightAwareApiUseCase = mock<SetEnableFlightAwareApiUseCase>(),
            setFlightAwareApiKeyUseCase = mock<SetFlightAwareApiKeyUseCase>(),
        )
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
            onNodeWithText("Show receiver locations").assertIsDisplayed()
            onNodeWithText("Show User Location on Map").assertIsDisplayed()
            onNodeWithText("Show Minimap Trails").assertIsDisplayed()
            onNodeWithText("Center map on user").assertIsDisplayed()
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
            onNodeWithText("Enable FlightAware API").assertIsDisplayed()
        }
}
