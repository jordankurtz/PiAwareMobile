package com.jordankurtz.piawaremobile.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TileProviderConfigTest {
    @Test
    fun osmUrlIsBuiltCorrectly() {
        val url = TileProviders.OPENSTREETMAP.buildUrl(zoom = 10, col = 512, row = 340)
        assertEquals("https://tile.openstreetmap.org/10/512/340.png", url)
    }

    @Test
    fun cartoUrlSubstitutesSubdomain() {
        val url = TileProviders.CARTO_DARK_ALL.buildUrl(zoom = 10, col = 512, row = 340, subdomain = "b")
        assertEquals("https://b.basemaps.cartocdn.com/dark_all/10/512/340.png", url)
    }

    @Test
    fun esriSatelliteUrlUsesRowBeforeCol() {
        // ESRI template is {z}/{y}/{x} — {y}=row, {x}=col
        val url = TileProviders.ESRI_SATELLITE.buildUrl(zoom = 10, col = 512, row = 340)
        assertEquals(
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/10/340/512",
            url,
        )
    }

    @Test
    fun esriTopoUrlUsesRowBeforeCol() {
        val url = TileProviders.ESRI_TOPO.buildUrl(zoom = 5, col = 2, row = 3)
        assertEquals(
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/5/3/2",
            url,
        )
    }

    @Test
    fun findByIdReturnsMatchingProvider() {
        val config = TileProviders.findById("carto_dark_all")
        assertEquals(TileProviders.CARTO_DARK_ALL, config)
    }

    @Test
    fun findByIdReturnsOsmForUnknownId() {
        val config = TileProviders.findById("unknown_provider_xyz")
        assertEquals(TileProviders.OPENSTREETMAP, config)
    }

    @Test
    fun allProviderIdsAreUnique() {
        val ids = TileProviders.ALL.map { it.id }
        assertEquals(ids, ids.distinct())
    }

}
