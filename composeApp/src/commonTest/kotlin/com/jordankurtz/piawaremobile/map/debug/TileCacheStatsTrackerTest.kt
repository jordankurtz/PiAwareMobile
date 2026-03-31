package com.jordankurtz.piawaremobile.map.debug

import kotlin.test.Test
import kotlin.test.assertEquals

class TileCacheStatsTrackerTest {
    @Test
    fun `initial state is all zeros`() {
        val tracker = TileCacheStatsTracker()

        val stats = tracker.stats.value
        assertEquals(0, stats.diskHits)
        assertEquals(0, stats.networkFetches)
        assertEquals(0, stats.errors)
    }

    @Test
    fun `recordDiskHit increments only diskHits`() {
        val tracker = TileCacheStatsTracker()

        tracker.recordDiskHit()

        val stats = tracker.stats.value
        assertEquals(1, stats.diskHits)
        assertEquals(0, stats.networkFetches)
        assertEquals(0, stats.errors)
    }

    @Test
    fun `recordNetworkFetch increments only networkFetches`() {
        val tracker = TileCacheStatsTracker()

        tracker.recordNetworkFetch()

        val stats = tracker.stats.value
        assertEquals(0, stats.diskHits)
        assertEquals(1, stats.networkFetches)
        assertEquals(0, stats.errors)
    }

    @Test
    fun `recordError increments only errors`() {
        val tracker = TileCacheStatsTracker()

        tracker.recordError()

        val stats = tracker.stats.value
        assertEquals(0, stats.diskHits)
        assertEquals(0, stats.networkFetches)
        assertEquals(1, stats.errors)
    }

    @Test
    fun `multiple calls accumulate correctly`() {
        val tracker = TileCacheStatsTracker()

        tracker.recordDiskHit()
        tracker.recordDiskHit()
        tracker.recordDiskHit()
        tracker.recordNetworkFetch()
        tracker.recordNetworkFetch()
        tracker.recordError()

        val stats = tracker.stats.value
        assertEquals(3, stats.diskHits)
        assertEquals(2, stats.networkFetches)
        assertEquals(1, stats.errors)
    }
}
