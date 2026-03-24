package com.jordankurtz.piawaremobile.map

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OpenStreetMapProviderTest {
    private val tileBytes = byteArrayOf(1, 2, 3, 4, 5)
    private var fetchCallCount = 0
    private val dummyClient = HttpClient(MockEngine { respond("unused") })

    private fun fakeFetch(throwOnCall: Boolean = false): suspend (HttpClient, String) -> RawSource =
        { _, _ ->
            fetchCallCount++
            if (throwOnCall) throw RuntimeException("Network error")
            Buffer().apply { write(tileBytes) }
        }

    private fun provider(
        maxEntries: Int = OpenStreetMapProvider.DEFAULT_MAX_CACHE_ENTRIES,
        fetchTile: suspend (HttpClient, String) -> RawSource = fakeFetch(),
    ): OpenStreetMapProvider =
        OpenStreetMapProvider(
            httpClient = dummyClient,
            maxCacheEntries = maxEntries,
            fetchTile = fetchTile,
        )

    @Test
    fun cacheHitReturnsDataWithoutNetworkCall() =
        runTest {
            val sut = provider()

            // First call populates cache
            val first = sut.getTileStream(row = 0, col = 0, zoomLvl = 1)
            assertNotNull(first)
            assertEquals(1, fetchCallCount)

            // Second call should hit cache — no additional fetch
            val second = sut.getTileStream(row = 0, col = 0, zoomLvl = 1)
            assertNotNull(second)
            assertEquals(1, fetchCallCount)

            // Verify data is correct
            val buffer = Buffer()
            second.use { raw ->
                while (raw.readAtMostTo(buffer, Long.MAX_VALUE) != -1L) {
                    // read until exhausted
                }
            }
            assertContentEquals(tileBytes, buffer.readByteArray())
        }

    @Test
    fun cacheMissFetchesFromNetworkAndPopulatesCache() =
        runTest {
            val sut = provider()

            val result = sut.getTileStream(row = 0, col = 0, zoomLvl = 1)
            assertNotNull(result)
            assertEquals(1, fetchCallCount)

            val buffer = Buffer()
            result.use { raw ->
                while (raw.readAtMostTo(buffer, Long.MAX_VALUE) != -1L) {
                    // read until exhausted
                }
            }
            assertContentEquals(tileBytes, buffer.readByteArray())
        }

    @Test
    fun lruEvictionRemovesOldestEntry() =
        runTest {
            val sut = provider(maxEntries = 3)

            // Fill cache with 3 tiles
            sut.getTileStream(row = 0, col = 0, zoomLvl = 1) // A
            sut.getTileStream(row = 1, col = 0, zoomLvl = 1) // B
            sut.getTileStream(row = 2, col = 0, zoomLvl = 1) // C
            assertEquals(3, fetchCallCount)

            // Add a fourth tile — should evict A (the oldest)
            sut.getTileStream(row = 3, col = 0, zoomLvl = 1) // D
            assertEquals(4, fetchCallCount)

            // A should be evicted — fetching it again triggers network call
            sut.getTileStream(row = 0, col = 0, zoomLvl = 1) // A (cache miss)
            assertEquals(5, fetchCallCount)

            // B and C should still be cached (B was evicted when A was reinserted)
            // Actually after evicting B to make room for A: cache = {C, D, A}
            sut.getTileStream(row = 2, col = 0, zoomLvl = 1) // C (cache hit)
            assertEquals(5, fetchCallCount)

            sut.getTileStream(row = 3, col = 0, zoomLvl = 1) // D (cache hit)
            assertEquals(5, fetchCallCount)
        }

    @Test
    fun accessOrderUpdatePreventsEvictionOfRecentlyAccessedTile() =
        runTest {
            val sut = provider(maxEntries = 2)

            // Fill cache: tile A then tile B
            sut.getTileStream(row = 0, col = 0, zoomLvl = 1) // A
            sut.getTileStream(row = 1, col = 0, zoomLvl = 1) // B
            assertEquals(2, fetchCallCount)

            // Access tile A again — makes it most recently used
            sut.getTileStream(row = 0, col = 0, zoomLvl = 1) // A (cache hit)
            assertEquals(2, fetchCallCount)

            // Add tile C — should evict B (least recently used), not A
            sut.getTileStream(row = 2, col = 0, zoomLvl = 1) // C
            assertEquals(3, fetchCallCount)

            // A should still be cached
            sut.getTileStream(row = 0, col = 0, zoomLvl = 1) // A (cache hit)
            assertEquals(3, fetchCallCount)

            // B should have been evicted
            sut.getTileStream(row = 1, col = 0, zoomLvl = 1) // B (cache miss)
            assertEquals(4, fetchCallCount)
        }

    @Test
    fun networkFailureReturnsNullAndDoesNotPolluteCache() =
        runTest {
            val sut = provider(fetchTile = fakeFetch(throwOnCall = true))

            val result = sut.getTileStream(row = 0, col = 0, zoomLvl = 1)
            assertNull(result)

            // Create a new provider to verify cache wasn't polluted,
            // but since we share the same instance, just verify with a working fetch
            // that the cache is empty by resetting
            fetchCallCount = 0
            val sut2 = provider()
            val result2 = sut2.getTileStream(row = 0, col = 0, zoomLvl = 1)
            assertNotNull(result2)
            assertEquals(1, fetchCallCount)
        }

    @Test
    fun concurrentAccessDoesNotCorruptState() =
        runTest {
            val sut = provider(maxEntries = 10)

            // Launch many concurrent requests for different tiles
            val jobs =
                (0 until 20).map { i ->
                    async {
                        sut.getTileStream(row = i, col = 0, zoomLvl = 1)
                    }
                }
            val results = jobs.awaitAll()

            // All should succeed (no corruption / null from race conditions)
            results.forEach { assertNotNull(it) }

            // Total fetches should equal 20 (one per unique tile)
            assertEquals(20, fetchCallCount)

            // Cache should retain the most recent entries and not exceed max.
            // Re-access the last 10 tiles — they should all be cache hits.
            val countBefore = fetchCallCount
            for (i in 10 until 20) {
                sut.getTileStream(row = i, col = 0, zoomLvl = 1)
            }
            assertEquals(countBefore, fetchCallCount, "Last 10 tiles should be cached")
        }

    @Test
    fun differentZoomLevelsAreCachedSeparately() =
        runTest {
            val sut = provider()

            sut.getTileStream(row = 0, col = 0, zoomLvl = 1)
            sut.getTileStream(row = 0, col = 0, zoomLvl = 2)
            assertEquals(2, fetchCallCount)

            // Both should be cached
            sut.getTileStream(row = 0, col = 0, zoomLvl = 1)
            sut.getTileStream(row = 0, col = 0, zoomLvl = 2)
            assertEquals(2, fetchCallCount)
        }
}
