package com.jordankurtz.piawaremobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.jordankurtz.piawaremobile.aircraft.cache.CachedRegion
import com.jordankurtz.piawaremobile.aircraft.cache.ui.FlightCacheScreen
import com.jordankurtz.piawaremobile.map.offline.BoundingBox
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock

class FlightCacheScreenAndroidTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun emptyStateRendersOnAndroid() {
        rule.setContent {
            FlightCacheScreen(onBack = {}, regions = emptyList())
        }
        rule.onNodeWithText("No cached regions").assertIsDisplayed()
    }

    @Test
    fun regionListRendersOnAndroid() {
        val region =
            CachedRegion(
                id = 1L,
                name = "Europe",
                box = BoundingBox(30.0, 55.0, -10.0, 30.0),
                daysAhead = 3,
                flightCount = 142,
                cachedAt = Clock.System.now(),
            )
        rule.setContent {
            FlightCacheScreen(onBack = {}, regions = listOf(region))
        }
        rule.onNodeWithText("Europe").assertIsDisplayed()
    }
}
