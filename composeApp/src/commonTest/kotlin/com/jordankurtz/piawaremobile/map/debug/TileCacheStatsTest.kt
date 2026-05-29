package com.jordankurtz.piawaremobile.map.debug

import kotlin.test.Test
import kotlin.test.assertEquals

class TileCacheStatsTest {
    @Test
    fun `total returns sum of diskHits and networkFetches`() {
        val stats = TileCacheStats(diskHits = 3L, networkFetches = 7L, errors = 2L)

        assertEquals(10L, stats.total)
    }

    @Test
    fun `total excludes errors from count`() {
        val stats = TileCacheStats(diskHits = 5L, networkFetches = 5L, errors = 100L)

        assertEquals(10L, stats.total)
    }

    @Test
    fun `hitRate returns 0 when total is zero`() {
        val stats = TileCacheStats(diskHits = 0L, networkFetches = 0L, errors = 0L)

        assertEquals(0f, stats.hitRate)
    }

    @Test
    fun `hitRate returns 0 when total is zero but errors exist`() {
        val stats = TileCacheStats(diskHits = 0L, networkFetches = 0L, errors = 5L)

        assertEquals(0f, stats.hitRate)
    }

    @Test
    fun `hitRate returns 1 when all requests are disk hits`() {
        val stats = TileCacheStats(diskHits = 10L, networkFetches = 0L)

        assertEquals(1f, stats.hitRate)
    }

    @Test
    fun `hitRate returns 0 when all requests are network fetches`() {
        val stats = TileCacheStats(diskHits = 0L, networkFetches = 10L)

        assertEquals(0f, stats.hitRate)
    }

    @Test
    fun `hitRate returns correct ratio for mixed values`() {
        val stats = TileCacheStats(diskHits = 3L, networkFetches = 7L)

        assertEquals(0.3f, stats.hitRate)
    }

    @Test
    fun `default values are all zero`() {
        val stats = TileCacheStats()

        assertEquals(0L, stats.diskHits)
        assertEquals(0L, stats.networkFetches)
        assertEquals(0L, stats.errors)
        assertEquals(0L, stats.total)
        assertEquals(0f, stats.hitRate)
    }
}
