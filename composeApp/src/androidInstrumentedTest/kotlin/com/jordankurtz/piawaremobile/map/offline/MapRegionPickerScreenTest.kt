package com.jordankurtz.piawaremobile.map.offline

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class MapRegionPickerScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pickerRendersConfirmAndCancelButtons() {
        composeTestRule.setContent {
            MapRegionPickerContent(onRegionSelected = {}, onDismiss = {}, mapLayer = {})
        }
        composeTestRule.onNodeWithText("Confirm", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel", substring = true).assertIsDisplayed()
    }
}
