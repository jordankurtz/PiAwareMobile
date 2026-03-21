package com.jordankurtz.piawaremobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.SettingsViewModel
import com.jordankurtz.piawaremobile.settings.ui.MainScreen
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
        val loadSettingsUseCase =
            mock<LoadSettingsUseCase> {
                every { invoke() } returns flowOf(Async.Success(Settings()))
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
    fun settingsScreenRenders() {
        composeTestRule.setContent {
            MainScreen(onServersClicked = {}, viewModel = createViewModel())
        }
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Servers").assertIsDisplayed()
        composeTestRule.onNodeWithText("Preferences").assertIsDisplayed()
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
        composeTestRule.onNodeWithText("Servers").performClick()
        assertTrue(clicked)
    }
}
