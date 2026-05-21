package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.map.TileProviders
import org.jetbrains.compose.resources.stringResource
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class MainScreenTest {
    @Test
    fun providerDropdownShowsSelectedProviderName() =
        runComposeUiTest {
            setContent {
                SettingsDropdown(
                    title = "Map Tiles",
                    description = "Tile source used for the map",
                    selectedValue = TileProviders.OPENSTREETMAP,
                    values = TileProviders.ALL.toTypedArray(),
                    onValueSelected = {},
                    stringFor = { stringResource(it.displayNameRes) },
                )
            }

            onNodeWithText("Map Tiles").assertIsDisplayed()
            onNodeWithText("OpenStreetMap").assertIsDisplayed()
        }

    @Test
    fun providerDropdownShowsEsriSatelliteWhenSelected() =
        runComposeUiTest {
            setContent {
                SettingsDropdown(
                    title = "Map Tiles",
                    description = "Tile source used for the map",
                    selectedValue = TileProviders.ESRI_SATELLITE,
                    values = TileProviders.ALL.toTypedArray(),
                    onValueSelected = {},
                    stringFor = { stringResource(it.displayNameRes) },
                )
            }

            onNodeWithText("ESRI Satellite").assertIsDisplayed()
        }

    @Test
    fun zoomDefaultInputIsDisplayed() =
        runComposeUiTest {
            setContent {
                SettingsNumberInput(title = "Default Zoom", value = 8, onValueChange = {}, range = 1..16)
            }
            onNodeWithText("Default Zoom").assertIsDisplayed()
        }

    @Test
    fun zoomInputOutOfRangeDoesNotFireCallback() =
        runComposeUiTest {
            var fired = false
            setContent {
                SettingsNumberInput(
                    title = "Min Zoom",
                    value = 1,
                    onValueChange = { fired = true },
                    range = 1..16,
                )
            }
            onNodeWithText("Min Zoom").assertIsDisplayed()
            assert(!fired)
        }
}
