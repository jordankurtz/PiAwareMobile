package com.jordankurtz.piawaremobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jordankurtz.piawaremobile.map.cache.TileCache
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.SettingsViewModel
import com.jordankurtz.piawaremobile.settings.ui.MapProvidersScreen
import com.jordankurtz.piawaremobile.settings.usecase.SettingsService
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapProvidersScreenAndroidTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createViewModel(): SettingsViewModel {
        val settingsService = mock<SettingsService>()
        every { settingsService.loadSettings() } returns flowOf(Async.Loading)
        return SettingsViewModel(settingsService, mock<TileCache>())
    }

    @Test
    fun mapProvidersScreen_displaysProviders() {
        composeTestRule.setContent {
            MapProvidersScreen(onBack = {}, viewModel = createViewModel())
        }
        composeTestRule.onNodeWithText("Map Providers").assertIsDisplayed()
        composeTestRule.onNodeWithText("OpenFreeMap Bright").assertIsDisplayed()
    }

    @Test
    fun screenShowsApiKeyRequiredBadge() {
        composeTestRule.setContent {
            MapProvidersScreen(onBack = {}, viewModel = createViewModel())
        }
        composeTestRule.onNodeWithText("Stadia Alidade Smooth").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("API key required")[0].assertIsDisplayed()
    }
}
