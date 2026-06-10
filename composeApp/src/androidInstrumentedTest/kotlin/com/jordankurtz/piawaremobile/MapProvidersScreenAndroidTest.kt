package com.jordankurtz.piawaremobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jordankurtz.piawaremobile.settings.SettingsViewModel
import com.jordankurtz.piawaremobile.settings.ui.MapProvidersScreen
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapProvidersScreenAndroidTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createViewModel(): SettingsViewModel {
        val settingsViewModel = mockk<SettingsViewModel>(relaxed = true)
        every { settingsViewModel.settings } returns MutableStateFlow(null)
        return settingsViewModel
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
