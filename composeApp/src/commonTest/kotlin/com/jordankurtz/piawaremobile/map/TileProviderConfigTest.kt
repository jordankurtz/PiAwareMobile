package com.jordankurtz.piawaremobile.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TileProviderConfigTest {
    @Test
    fun `OPENFREEMAP_BRIGHT has expected styleUrl`() {
        assertEquals("https://tiles.openfreemap.org/styles/bright", TileProviders.OPENFREEMAP_BRIGHT.styleUrl)
    }

    @Test
    fun `STADIA_ALIDADE_SMOOTH requires api key`() {
        assertTrue(TileProviders.STADIA_ALIDADE_SMOOTH.requiresApiKey)
        assertEquals("stadia", TileProviders.STADIA_ALIDADE_SMOOTH.apiKeyGroup)
    }

    @Test
    fun `OPENFREEMAP_BRIGHT does not require api key`() {
        assertFalse(TileProviders.OPENFREEMAP_BRIGHT.requiresApiKey)
    }

    @Test
    fun `TileProviders ALL contains six providers`() {
        assertEquals(6, TileProviders.ALL.size)
    }

    @Test
    fun `all provider ids are unique`() {
        val ids = TileProviders.ALL.map { it.id }
        assertEquals(ids, ids.distinct())
    }

    @Test
    fun `resolvedStyleUrl substitutes api key`() {
        val config =
            TileProviderConfig(
                id = "test",
                displayName = "Test",
                styleUrl = "https://example.com/style.json?api_key={api_key}",
                requiresApiKey = true,
            )
        val resolved = config.resolvedStyleUrl("my-secret-key")
        assertEquals("https://example.com/style.json?api_key=my-secret-key", resolved)
    }

    @Test
    fun `MAPTILER_STREETS has maptiler apiKeyGroup`() {
        assertEquals("maptiler", TileProviders.MAPTILER_STREETS.apiKeyGroup)
        assertTrue(TileProviders.MAPTILER_STREETS.requiresApiKey)
    }

    @Test
    fun `DEFAULT is OPENFREEMAP_BRIGHT`() {
        assertEquals(TileProviders.OPENFREEMAP_BRIGHT, TileProviders.DEFAULT)
    }
}
