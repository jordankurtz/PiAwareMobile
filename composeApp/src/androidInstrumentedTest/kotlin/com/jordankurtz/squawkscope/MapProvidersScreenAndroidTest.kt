package com.jordankurtz.squawkscope

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.jordankurtz.squawkscope.map.cache.TileCache
import com.jordankurtz.squawkscope.model.Async
import com.jordankurtz.squawkscope.settings.Settings
import com.jordankurtz.squawkscope.settings.SettingsViewModel
import com.jordankurtz.squawkscope.settings.ui.MapProvidersScreen
import com.jordankurtz.squawkscope.settings.usecase.SettingsService
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
        composeTestRule.onAllNodesWithText("API key required")[0].assertIsDisplayed()
    }
}
