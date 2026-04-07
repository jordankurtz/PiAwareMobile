package com.jordankurtz.piawaremobile

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.map.TileProviders
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OverlayTest {
    @Test
    fun showsProviderAttribution() =
        runComposeUiTest {
            setContent {
                Overlay(
                    numberOfPlanes = 5,
                    provider = TileProviders.OPENSTREETMAP,
                    modifier = androidx.compose.ui.Modifier,
                )
            }
            onNodeWithText("© OpenStreetMap contributors").assertIsDisplayed()
        }

    @Test
    fun showsCartoAttributionForDarkProvider() =
        runComposeUiTest {
            setContent {
                Overlay(
                    numberOfPlanes = 3,
                    provider = TileProviders.CARTO_DARK_ALL,
                    modifier = androidx.compose.ui.Modifier,
                )
            }
            onNodeWithText("© CARTO, © OpenStreetMap contributors").assertIsDisplayed()
        }
}
