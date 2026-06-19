package com.jordankurtz.piawaremobile.aircraft.cache.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.aircraft.cache.CachedRegion
import com.jordankurtz.piawaremobile.map.offline.BoundingBox
import kotlin.test.Test
import kotlin.time.Clock

@OptIn(ExperimentalTestApi::class)
class FlightCacheScreenTest {
    @Test
    fun `empty state shows empty title`() =
        runComposeUiTest {
            setContent {
                FlightCacheScreen(onBack = {}, regions = emptyList())
            }
            onNodeWithText("No cached regions").assertExists()
        }

    @Test
    fun `region list shows region name and flight count`() =
        runComposeUiTest {
            val region =
                CachedRegion(
                    id = 1L,
                    name = "Europe",
                    box = BoundingBox(30.0, 55.0, -10.0, 30.0),
                    daysAhead = 3,
                    flightCount = 142,
                    cachedAt = Clock.System.now(),
                )
            setContent {
                FlightCacheScreen(onBack = {}, regions = listOf(region))
            }
            onNodeWithText("Europe").assertExists()
            onNodeWithText("142 flights").assertExists()
        }
}
