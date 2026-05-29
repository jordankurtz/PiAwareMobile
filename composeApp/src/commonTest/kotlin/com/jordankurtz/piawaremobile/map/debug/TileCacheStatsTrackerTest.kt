package com.jordankurtz.piawaremobile.map.debug

import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TileCacheStatsTrackerTest {
    @Test
    fun `initial state is all zeros`() {
        val tracker = TileCacheStatsTracker()

        val stats = tracker.stats.value
        assertEquals(0L, stats.diskHits)
        assertEquals(0L, stats.networkFetches)
        assertEquals(0L, stats.errors)
    }

    @Test
    fun `recordDiskHit increments only diskHits`() {
        val tracker = TileCacheStatsTracker()

        tracker.recordDiskHit()

        val stats = tracker.stats.value
        assertEquals(1L, stats.diskHits)
        assertEquals(0L, stats.networkFetches)
        assertEquals(0L, stats.errors)
    }

    @Test
    fun `recordNetworkFetch increments only networkFetches`() {
        val tracker = TileCacheStatsTracker()

        tracker.recordNetworkFetch()

        val stats = tracker.stats.value
        assertEquals(0L, stats.diskHits)
        assertEquals(1L, stats.networkFetches)
        assertEquals(0L, stats.errors)
    }

    @Test
    fun `recordError increments only errors`() {
        val tracker = TileCacheStatsTracker()

        tracker.recordError()

        val stats = tracker.stats.value
        assertEquals(0L, stats.diskHits)
        assertEquals(0L, stats.networkFetches)
        assertEquals(1L, stats.errors)
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
        assertEquals(3L, stats.diskHits)
        assertEquals(2L, stats.networkFetches)
        assertEquals(1L, stats.errors)
    }

    @Test
    fun `concurrent recordDiskHit calls accumulate correctly`() =
        runTest {
            val tracker = TileCacheStatsTracker()

            (1..100).map {
                launch { tracker.recordDiskHit() }
            }.joinAll()

            assertEquals(100L, tracker.stats.value.diskHits)
        }
}
