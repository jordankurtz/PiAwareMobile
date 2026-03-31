package com.jordankurtz.piawaremobile.map.debug

import kotlin.test.Test
import kotlin.test.assertEquals

class TileCacheStatsTest {
    @Test
    fun `total returns sum of diskHits and networkFetches`() {
        val stats = TileCacheStats(diskHits = 3, networkFetches = 7, errors = 2)

        assertEquals(10, stats.total)
    }

    @Test
    fun `total excludes errors from count`() {
        val stats = TileCacheStats(diskHits = 5, networkFetches = 5, errors = 100)

        assertEquals(10, stats.total)
    }

    @Test
    fun `hitRate returns 0 when total is zero`() {
        val stats = TileCacheStats(diskHits = 0, networkFetches = 0, errors = 0)

        assertEquals(0f, stats.hitRate)
    }

    @Test
    fun `hitRate returns 0 when total is zero but errors exist`() {
        val stats = TileCacheStats(diskHits = 0, networkFetches = 0, errors = 5)

        assertEquals(0f, stats.hitRate)
    }

    @Test
    fun `hitRate returns 1 when all requests are disk hits`() {
        val stats = TileCacheStats(diskHits = 10, networkFetches = 0)

        assertEquals(1f, stats.hitRate)
    }

    @Test
    fun `hitRate returns 0 when all requests are network fetches`() {
        val stats = TileCacheStats(diskHits = 0, networkFetches = 10)

        assertEquals(0f, stats.hitRate)
    }

    @Test
    fun `hitRate returns correct ratio for mixed values`() {
        val stats = TileCacheStats(diskHits = 3, networkFetches = 7)

        assertEquals(0.3f, stats.hitRate)
    }

    @Test
    fun `default values are all zero`() {
        val stats = TileCacheStats()

        assertEquals(0, stats.diskHits)
        assertEquals(0, stats.networkFetches)
        assertEquals(0, stats.errors)
        assertEquals(0, stats.total)
        assertEquals(0f, stats.hitRate)
    }
}
