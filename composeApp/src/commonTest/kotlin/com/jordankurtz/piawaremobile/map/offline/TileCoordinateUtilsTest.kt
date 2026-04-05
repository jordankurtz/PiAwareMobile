package com.jordankurtz.piawaremobile.map.offline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TileCoordinateUtilsTest {
    @Test
    fun `latLonToTile returns correct col and row for NYC at zoom 10`() {
        // New York City: 40.7128°N, 74.0060°W
        val (col, row) = latLonToTile(lat = 40.7128, lon = -74.0060, zoom = 10)
        assertEquals(301, col)
        assertEquals(385, row)
    }

    @Test
    fun `latLonToTile returns correct col and row at zoom 0`() {
        // Anywhere at zoom 0 is tile (0, 0)
        val (col, row) = latLonToTile(lat = 0.0, lon = 0.0, zoom = 0)
        assertEquals(0, col)
        assertEquals(0, row)
    }

    @Test
    fun `latLonToTile clamps to valid range`() {
        // Extreme coordinates should stay within [0, 2^zoom - 1]
        val zoom = 5
        val max = (1 shl zoom) - 1
        val (col, row) = latLonToTile(lat = 85.0, lon = 180.0, zoom = zoom)
        assertTrue(col in 0..max)
        assertTrue(row in 0..max)
    }

    @Test
    fun `tileCount returns correct count for 1x1 degree box at zoom 8`() {
        // Box: 40-41°N, 75-74°W → 4 tiles at zoom 8
        val bounds = BoundingBox(minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0)
        assertEquals(4L, tileCount(bounds = bounds, minZoom = 8, maxZoom = 8))
    }

    @Test
    fun `tileCount sums across multiple zoom levels`() {
        val bounds = BoundingBox(minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0)
        val total = tileCount(bounds = bounds, minZoom = 8, maxZoom = 9)
        // 4 tiles at zoom 8 + 6 tiles at zoom 9 = 10 total
        assertEquals(10L, total)
    }

    @Test
    fun `tilesForRegion yields all tiles within bounds at single zoom`() {
        val bounds = BoundingBox(minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0)
        val tiles = tilesForRegion(bounds = bounds, minZoom = 8, maxZoom = 8).toList()
        assertEquals(4, tiles.size)
        assertTrue(tiles.all { it.zoom == 8 })
    }

    @Test
    fun `tilesForRegion yields tiles for all zoom levels`() {
        val bounds = BoundingBox(minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0)
        val tiles = tilesForRegion(bounds = bounds, minZoom = 8, maxZoom = 9).toList()
        val zooms = tiles.map { it.zoom }.toSet()
        assertEquals(setOf(8, 9), zooms)
    }
}
