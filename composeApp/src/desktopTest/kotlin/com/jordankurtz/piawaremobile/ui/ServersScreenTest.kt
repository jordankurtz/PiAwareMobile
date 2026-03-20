package com.jordankurtz.piawaremobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.SettingsViewModel
import com.jordankurtz.piawaremobile.settings.ui.ServersScreen
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
import com.jordankurtz.piawaremobile.testutil.mockServer
import com.jordankurtz.piawaremobile.testutil.mockSettings
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ServersScreenTest {
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
    fun displaysServersTitle() =
        runComposeUiTest {
            setContent {
                ServersScreen(onBack = {}, viewModel = createViewModel())
            }
            onNodeWithText("Servers").assertIsDisplayed()
        }

    @Test
    fun displaysServerList() =
        runComposeUiTest {
            val settings =
                mockSettings(
                    servers =
                        listOf(
                            mockServer(name = "Home PiAware", address = "piaware.local"),
                            mockServer(name = "Remote PiAware", address = "remote.local"),
                        ),
                )
            setContent {
                ServersScreen(onBack = {}, viewModel = createViewModel(settings))
            }
            onNodeWithText("Home PiAware").assertIsDisplayed()
            onNodeWithText("Remote PiAware").assertIsDisplayed()
        }

    @Test
    fun backButtonFiresCallback() =
        runComposeUiTest {
            var backClicked = false
            setContent {
                ServersScreen(onBack = { backClicked = true }, viewModel = createViewModel())
            }
            onNodeWithContentDescription("Back").performClick()
            assertTrue(backClicked)
        }

    @Test
    fun addButtonOpensDialog() =
        runComposeUiTest {
            setContent {
                ServersScreen(onBack = {}, viewModel = createViewModel())
            }
            onNodeWithContentDescription("Add Server").performClick()
            onNodeWithText("Enter Details").assertIsDisplayed()
            onNodeWithText("Name*").assertIsDisplayed()
            onNodeWithText("Address*").assertIsDisplayed()
        }

    @Test
    fun emptyServerListShowsNoItems() =
        runComposeUiTest {
            val settings = mockSettings(servers = emptyList())
            setContent {
                ServersScreen(onBack = {}, viewModel = createViewModel(settings))
            }
            onNodeWithText("Servers").assertIsDisplayed()
        }
}
