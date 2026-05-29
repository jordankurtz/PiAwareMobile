package com.jordankurtz.piawaremobile.extensions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import com.jordankurtz.piawaremobile.map.TileProviders
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ModelExtensionsTest {
    @Test
    fun overlayColorIsWhiteForDarkProvider() {
        assertEquals(Color.White, TileProviders.CARTO_DARK_ALL.overlayColor)
    }

    @Test
    fun overlayColorIsBlackForLightProvider() {
        assertEquals(Color.Black, TileProviders.OPENSTREETMAP.overlayColor)
    }
}
