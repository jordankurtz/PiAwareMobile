package com.jordankurtz.piawaremobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.SettingsViewModel
import com.jordankurtz.piawaremobile.settings.ui.AddServerDialog
import com.jordankurtz.piawaremobile.settings.ui.ServersScreen
import com.jordankurtz.piawaremobile.settings.usecase.AddServerUseCase
import com.jordankurtz.piawaremobile.settings.usecase.DeleteServerUseCase
import com.jordankurtz.piawaremobile.settings.usecase.EditServerUseCase
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class InteractiveUiTest {
    private fun createViewModel(settings: Settings = mockSettings()): SettingsViewModel {
        val loadSettingsUseCase =
            mock<LoadSettingsUseCase> {
                every { invoke() } returns flowOf(Async.Success(settings))
            }

        return SettingsViewModel(
            loadSettingsUseCase = loadSettingsUseCase,
            addServerUseCase = mock<AddServerUseCase>(),
            editServerUseCase = mock<EditServerUseCase>(),
            deleteServerUseCase = mock<DeleteServerUseCase>(),
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
    fun addServerDialogTypingEnablesSaveButton() =
        runComposeUiTest {
            setContent {
                AddServerDialog(onDismiss = {}, onConfirm = { _, _ -> })
            }
            onNodeWithText("Name").performClick()
            onNodeWithText("Name").performTextInput("My PiAware")
            onNodeWithText("Address").performClick()
            onNodeWithText("Address").performTextInput("piaware.local")
            onNodeWithText("Save").assertIsEnabled()
        }

    @Test
    fun addServerDialogSaveReturnsInputValues() =
        runComposeUiTest {
            var savedName = ""
            var savedAddress = ""
            setContent {
                AddServerDialog(
                    onDismiss = {},
                    onConfirm = { name, address ->
                        savedName = name
                        savedAddress = address
                    },
                )
            }
            onNodeWithText("Name").performClick()
            onNodeWithText("Name").performTextInput("Test Server")
            onNodeWithText("Address").performClick()
            onNodeWithText("Address").performTextInput("test.local")
            onNodeWithText("Save").performClick()
            assertEquals("Test Server", savedName)
            assertEquals("test.local", savedAddress)
        }

    @Test
    fun serversScreenAddDialogFlow() =
        runComposeUiTest {
            val settings = mockSettings(servers = emptyList())
            setContent {
                ServersScreen(onBack = {}, viewModel = createViewModel(settings))
            }
            onNodeWithText("Servers").assertIsDisplayed()
            onNodeWithContentDescription("Add server").performClick()
            onNodeWithText("Add Server").assertIsDisplayed()
            onNodeWithText("Name").performClick()
            onNodeWithText("Name").performTextInput("New Server")
            onNodeWithText("Address").performClick()
            onNodeWithText("Address").performTextInput("new.local")
            onNodeWithText("Save").assertIsEnabled()
        }

    @Test
    fun serversScreenDeleteConfirmFlow() =
        runComposeUiTest {
            val settings =
                mockSettings(
                    servers = listOf(mockServer(name = "Test PiAware", address = "test.local")),
                )
            setContent {
                ServersScreen(onBack = {}, viewModel = createViewModel(settings))
            }
            onNodeWithText("Test PiAware").assertIsDisplayed()
            onNodeWithContentDescription("Delete server").performClick()
            onNodeWithText("Delete Server").assertIsDisplayed()
            onNodeWithText("Are you sure you want to delete \"Test PiAware\"?").assertIsDisplayed()
            onNodeWithText("Cancel").assertIsDisplayed()
            onNodeWithText("Delete").assertIsDisplayed()
        }

    @Test
    fun serversScreenDeleteCancelDismissesDialog() =
        runComposeUiTest {
            val settings =
                mockSettings(
                    servers = listOf(mockServer(name = "Test PiAware", address = "test.local")),
                )
            setContent {
                ServersScreen(onBack = {}, viewModel = createViewModel(settings))
            }
            onNodeWithContentDescription("Delete server").performClick()
            onNodeWithText("Delete Server").assertIsDisplayed()
            onNodeWithText("Cancel").performClick()
            onNodeWithText("Test PiAware").assertIsDisplayed()
        }

    @Test
    fun serversScreenEditDialogOpensWithTitle() =
        runComposeUiTest {
            val settings =
                mockSettings(
                    servers = listOf(mockServer(name = "Home Pi", address = "home.local")),
                )
            setContent {
                ServersScreen(onBack = {}, viewModel = createViewModel(settings))
            }
            onNodeWithContentDescription("Edit server").performClick()
            onNodeWithText("Edit Server").assertIsDisplayed()
            onNodeWithText("Name").assertIsDisplayed()
            onNodeWithText("Address").assertIsDisplayed()
        }

    @Test
    fun serversScreenBackButtonNavigates() =
        runComposeUiTest {
            var backClicked = false
            setContent {
                ServersScreen(
                    onBack = { backClicked = true },
                    viewModel = createViewModel(),
                )
            }
            onNodeWithContentDescription("Back").performClick()
            assertTrue(backClicked)
        }
}
