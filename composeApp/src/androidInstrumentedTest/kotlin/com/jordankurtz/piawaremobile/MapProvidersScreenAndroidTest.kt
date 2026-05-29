package com.jordankurtz.piawaremobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.jordankurtz.piawaremobile.map.cache.TileCache
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.SettingsViewModel
import com.jordankurtz.piawaremobile.settings.ui.MapProvidersScreen
import com.jordankurtz.piawaremobile.settings.usecase.SettingsService
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class MapProvidersScreenAndroidTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createViewModel(): SettingsViewModel {
        val service =
            mock<SettingsService> {
                every { loadSettings() } returns flowOf(Async.Success(Settings()))
            }
        return SettingsViewModel(settingsService = service, tileCache = mock<TileCache>())
    }

    @Test
    fun screenRendersWithBuiltInProviders() {
        composeTestRule.setContent {
            MapProvidersScreen(onBack = {}, viewModel = createViewModel())
        }
        composeTestRule.onNodeWithText("Map Providers").assertIsDisplayed()
        composeTestRule.onNodeWithText("OpenStreetMap").assertIsDisplayed()
    }

    @Test
    fun screenShowsApiKeyRequiredBadge() {
        composeTestRule.setContent {
            MapProvidersScreen(onBack = {}, viewModel = createViewModel())
        }
        composeTestRule.onNodeWithText("Stadia Toner").assertIsDisplayed()
        composeTestRule.onNodeWithText("API key required").assertIsDisplayed()
    }
}
