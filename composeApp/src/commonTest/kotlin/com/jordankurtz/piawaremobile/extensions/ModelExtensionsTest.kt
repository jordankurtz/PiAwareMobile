package com.jordankurtz.piawaremobile.extensions

import androidx.compose.ui.graphics.Color
import com.jordankurtz.piawaremobile.map.TileProviders
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelExtensionsTest {
    @Test
    fun `overlayColor is Black for light map provider`() {
        assertEquals(Color.Black, TileProviders.OPENSTREETMAP.overlayColor)
    }

    @Test
    fun `overlayColor is White for dark map provider`() {
        assertEquals(Color.White, TileProviders.CARTO_DARK_ALL.overlayColor)
    }
}
