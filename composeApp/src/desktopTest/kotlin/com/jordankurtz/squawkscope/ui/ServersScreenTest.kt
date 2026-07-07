package com.jordankurtz.squawkscope.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.squawkscope.map.cache.TileCache
import com.jordankurtz.squawkscope.model.Async
import com.jordankurtz.squawkscope.settings.Settings
import com.jordankurtz.squawkscope.settings.SettingsViewModel
import com.jordankurtz.squawkscope.settings.ui.ServersScreen
import com.jordankurtz.squawkscope.settings.usecase.SettingsService
import com.jordankurtz.squawkscope.testutil.mockServer
import com.jordankurtz.squawkscope.testutil.mockSettings
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ServersScreenTest {
    private fun createViewModel(settings: Settings = mockSettings()): SettingsViewModel {
        val settingsService =
            mock<SettingsService> {
                every { loadSettings() } returns flowOf(Async.Success(settings))
            }
        return SettingsViewModel(settingsService, mock<TileCache>())
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
            onNodeWithContentDescription("Add server").performClick()
            onNodeWithText("Add Server").assertIsDisplayed()
            onNodeWithText("Name").assertIsDisplayed()
            onNodeWithText("Address").assertIsDisplayed()
        }

    @Test
    fun editButtonOpensEditDialog() =
        runComposeUiTest {
            val settings =
                mockSettings(
                    servers = listOf(mockServer(name = "My PiAware", address = "piaware.local")),
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
    fun deleteButtonShowsConfirmation() =
        runComposeUiTest {
            val settings =
                mockSettings(
                    servers = listOf(mockServer(name = "My PiAware", address = "piaware.local")),
                )
            setContent {
                ServersScreen(onBack = {}, viewModel = createViewModel(settings))
            }
            onNodeWithContentDescription("Delete server").performClick()
            onNodeWithText("Delete Server").assertIsDisplayed()
            onNodeWithText("Are you sure you want to delete \"My PiAware\"?").assertIsDisplayed()
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
