package com.jordankurtz.piawaremobile.map.cache

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class FileTileCacheCommonTest {
    private lateinit var fakeFs: FakeCacheFileSystem
    private lateinit var queries: TileCacheQueries
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        fakeFs = FakeCacheFileSystem()
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TileCacheDatabase.Schema.create(driver)
        queries = TileCacheDatabase(driver).tileCacheQueries
    }

    private fun createCache(
        maxCacheBytes: Long = FileTileCache.DEFAULT_MAX_CACHE_BYTES,
        maxAgeMillis: Long = FileTileCache.DEFAULT_MAX_AGE_MILLIS,
        cacheScope: TestScope? = null,
    ): FileTileCache =
        FileTileCache(
            cacheFileSystem = fakeFs,
            queries = queries,
            ioDispatcher = testDispatcher,
            maxCacheBytes = maxCacheBytes,
            maxAgeMillis = maxAgeMillis,
            cacheScope = cacheScope,
        )

    @Test
    fun `get returns null for tile that has never been cached`() =
        runTest(testDispatcher) {
            val cache = createCache()

            val result = cache.get(zoomLvl = 1, col = 2, row = 3, providerId = "osm")

            assertNull(result)
        }

    @Test
    fun `put then get returns same ByteArray contents`() =
        runTest(testDispatcher) {
            val cache = createCache()
            val data = byteArrayOf(1, 2, 3, 4, 5)

            cache.put(zoomLvl = 5, col = 10, row = 20, providerId = "osm", data = data)
            val result = cache.get(zoomLvl = 5, col = 10, row = 20, providerId = "osm")

            assertNotNull(result)
            assertContentEquals(data, result)
        }

    @Test
    fun `tiles for different coordinates are stored and retrieved independently`() =
        runTest(testDispatcher) {
            val cache = createCache()
            val data1 = byteArrayOf(1, 2, 3)
            val data2 = byteArrayOf(4, 5, 6)

            cache.put(zoomLvl = 1, col = 0, row = 0, providerId = "osm", data = data1)
            cache.put(zoomLvl = 2, col = 0, row = 0, providerId = "osm", data = data2)

            assertContentEquals(data1, cache.get(zoomLvl = 1, col = 0, row = 0, providerId = "osm"))
            assertContentEquals(data2, cache.get(zoomLvl = 2, col = 0, row = 0, providerId = "osm"))
        }

    @Test
    fun `put overwrites existing tile data for same coordinates`() =
        runTest(testDispatcher) {
            val cache = createCache()
            val originalData = byteArrayOf(1, 2, 3)
            val updatedData = byteArrayOf(7, 8, 9, 10)

            cache.put(zoomLvl = 1, col = 0, row = 0, providerId = "osm", data = originalData)
            cache.put(zoomLvl = 1, col = 0, row = 0, providerId = "osm", data = updatedData)

            assertContentEquals(updatedData, cache.get(zoomLvl = 1, col = 0, row = 0, providerId = "osm"))
        }

    @Test
    fun `expired tile returns null and cleans up db entries`() =
        runTest(testDispatcher) {
            val cache = createCache(maxAgeMillis = 1L)
            val data = byteArrayOf(1, 2, 3)

            cache.put(zoomLvl = 1, col = 0, row = 0, providerId = "osm", data = data)

            // Backdate the fetched_at to simulate expiration
            queries.upsertTile(
                1L,
                0L,
                0L,
                "osm",
                data.size.toLong(),
                Clock.System.now().toEpochMilliseconds() - 1000,
            )

            val result = cache.get(zoomLvl = 1, col = 0, row = 0, providerId = "osm")

            assertNull(result)
            assertFalse(fakeFs.exists("osm/1/0/0.png"), "Expired tile file should be deleted")
        }

    @Test
    fun `evicts least recently accessed tiles when cache exceeds max size`() =
        runTest(testDispatcher) {
            val cache = createCache(maxCacheBytes = 10L, cacheScope = this)
            val data = byteArrayOf(1, 2, 3, 4, 5)

            cache.put(zoomLvl = 1, col = 0, row = 0, providerId = "osm", data = data)
            // Backdate the first tile's access time so it gets evicted first
            queries.updateLastAccessed(
                Clock.System.now().toEpochMilliseconds() - 5000,
                1L,
                0L,
                0L,
                "osm",
            )

            cache.put(zoomLvl = 1, col = 0, row = 1, providerId = "osm", data = data)

            // Third tile pushes cache to 15 bytes, exceeding 10-byte limit
            cache.put(zoomLvl = 1, col = 0, row = 2, providerId = "osm", data = data)
            advanceUntilIdle()

            // The oldest-accessed tile (row=0) should be evicted
            assertFalse(fakeFs.exists("osm/1/0/0.png"), "Oldest-accessed tile should be evicted")

            // Newer tiles should remain
            assertNotNull(cache.get(zoomLvl = 1, col = 0, row = 2, providerId = "osm"))
        }

    @Test
    fun `recently accessed tile survives eviction over less recently accessed tile`() =
        runTest(testDispatcher) {
            val cache = createCache(maxCacheBytes = 10L, cacheScope = this)
            val data = byteArrayOf(1, 2, 3, 4, 5)

            // Put tile A (5 bytes)
            cache.put(zoomLvl = 1, col = 0, row = 0, providerId = "osm", data = data)
            queries.updateLastAccessed(
                Clock.System.now().toEpochMilliseconds() - 10_000,
                1L,
                0L,
                0L,
                "osm",
            )

            // Put tile B (5 bytes) -- at 10 bytes, at limit
            cache.put(zoomLvl = 1, col = 0, row = 1, providerId = "osm", data = data)
            queries.updateLastAccessed(
                Clock.System.now().toEpochMilliseconds() - 5_000,
                1L,
                0L,
                1L,
                "osm",
            )

            // Access tile A to make it recently used
            cache.get(zoomLvl = 1, col = 0, row = 0, providerId = "osm")

            // Put tile C -- exceeds limit, should evict tile B (least recently accessed)
            cache.put(zoomLvl = 1, col = 0, row = 2, providerId = "osm", data = data)
            advanceUntilIdle()

            // Tile A should survive because it was recently accessed
            assertNotNull(
                cache.get(zoomLvl = 1, col = 0, row = 0, providerId = "osm"),
                "Recently accessed tile should survive eviction",
            )
            // Tile B should be evicted
            assertFalse(
                fakeFs.exists("osm/1/0/1.png"),
                "Least recently accessed tile should be evicted",
            )
        }

    @Test
    fun `get returns null on file I-O error`() =
        runTest(testDispatcher) {
            val cache = createCache()
            val data = byteArrayOf(1, 2, 3)

            cache.put(zoomLvl = 1, col = 0, row = 0, providerId = "osm", data = data)

            // Enable read errors -- file is missing from disk but DB entry exists
            fakeFs.throwOnRead = true

            val result = cache.get(zoomLvl = 1, col = 0, row = 0, providerId = "osm")

            assertNull(result, "get() should return null on file I/O error")
        }

    @Test
    fun `put handles file I-O error without crashing`() =
        runTest(testDispatcher) {
            val cache = createCache()

            fakeFs.throwOnWrite = true

            // Should not throw
            cache.put(zoomLvl = 1, col = 0, row = 0, providerId = "osm", data = byteArrayOf(1, 2, 3))
        }

    @Test
    fun `get returns empty ByteArray for zero-byte cached file`() =
        runTest(testDispatcher) {
            val cache = createCache()

            cache.put(zoomLvl = 1, col = 0, row = 0, providerId = "osm", data = byteArrayOf())
            val result = cache.get(zoomLvl = 1, col = 0, row = 0, providerId = "osm")

            assertNotNull(result)
            assertContentEquals(byteArrayOf(), result)
        }

    @Test
    fun `eviction does not run when cache size is within limit`() =
        runTest(testDispatcher) {
            val cache = createCache(maxCacheBytes = 1000L, cacheScope = this)
            val data = byteArrayOf(1, 2, 3, 4, 5)

            cache.put(zoomLvl = 1, col = 0, row = 0, providerId = "osm", data = data)
            cache.put(zoomLvl = 1, col = 0, row = 1, providerId = "osm", data = data)
            advanceUntilIdle()

            // Both tiles should still exist (total 10 bytes, well under 1000 limit)
            assertTrue(fakeFs.exists("osm/1/0/0.png"), "Tile should not be evicted when under limit")
            assertTrue(fakeFs.exists("osm/1/0/1.png"), "Tile should not be evicted when under limit")
        }

    @Test
    fun `expired tile is not served even if recently accessed`() =
        runTest(testDispatcher) {
            val cache = createCache(maxAgeMillis = 1L)
            val data = byteArrayOf(1, 2, 3)

            cache.put(zoomLvl = 1, col = 0, row = 0, providerId = "osm", data = data)

            // Backdate fetched_at to simulate expiration
            queries.upsertTile(
                1L,
                0L,
                0L,
                "osm",
                data.size.toLong(),
                Clock.System.now().toEpochMilliseconds() - 1000,
            )
            // Set access time to recent (shouldn't matter -- expiration wins)
            queries.updateLastAccessed(
                Clock.System.now().toEpochMilliseconds(),
                1L,
                0L,
                0L,
                "osm",
            )

            val result = cache.get(zoomLvl = 1, col = 0, row = 0, providerId = "osm")

            assertNull(result, "Expired tile should not be served regardless of access time")
        }

    @Test
    fun `eviction leaves cache at or below max size`() =
        runTest(testDispatcher) {
            val cache = createCache(maxCacheBytes = 10L, cacheScope = this)

            // Put 4 tiles of 5 bytes each: total 20 bytes, exceeding 10-byte limit
            cache.put(zoomLvl = 1, col = 0, row = 0, providerId = "osm", data = byteArrayOf(1, 2, 3, 4, 5))
            queries.updateLastAccessed(
                Clock.System.now().toEpochMilliseconds() - 20_000,
                1L,
                0L,
                0L,
                "osm",
            )

            cache.put(zoomLvl = 1, col = 0, row = 1, providerId = "osm", data = byteArrayOf(1, 2, 3, 4, 5))
            queries.updateLastAccessed(
                Clock.System.now().toEpochMilliseconds() - 15_000,
                1L,
                0L,
                1L,
                "osm",
            )

            cache.put(zoomLvl = 1, col = 0, row = 2, providerId = "osm", data = byteArrayOf(1, 2, 3, 4, 5))
            queries.updateLastAccessed(
                Clock.System.now().toEpochMilliseconds() - 10_000,
                1L,
                0L,
                2L,
                "osm",
            )

            cache.put(zoomLvl = 1, col = 0, row = 3, providerId = "osm", data = byteArrayOf(1, 2, 3, 4, 5))
            advanceUntilIdle()

            // After eviction, total size in DB should be at or below 10 bytes
            val totalSize = queries.totalCacheSize().executeAsOne()
            assertTrue(
                totalSize <= 10L,
                "Cache size should be at or below maxCacheBytes after eviction, " +
                    "but was $totalSize",
            )
        }

    @Test
    fun `pinned tiles are not evicted`() =
        runTest(testDispatcher) {
            val cache = createCache(maxCacheBytes = 10L, cacheScope = this)

            // Put a tile and pin it
            val data = byteArrayOf(1, 2, 3, 4, 5)
            cache.put(zoomLvl = 5, col = 1, row = 1, providerId = "osm", data = data)
            queries.insertPinnedTile(
                zoom_level = 5L,
                col = 1L,
                row = 1L,
                provider_id = "osm",
                // FK enforcement is off in JVM SQLite tests
                region_id = 1L,
            )

            // Put more tiles to exceed the 10-byte limit and trigger eviction
            cache.put(zoomLvl = 5, col = 2, row = 1, providerId = "osm", data = data)
            cache.put(zoomLvl = 5, col = 3, row = 1, providerId = "osm", data = data)
            advanceUntilIdle()

            // The pinned tile should NOT be evicted
            val result = cache.get(zoomLvl = 5, col = 1, row = 1, providerId = "osm")
            assertNotNull(result, "Pinned tile should not be evicted")
        }

    @Test
    fun `get cleans up db when file is missing from disk`() =
        runTest(testDispatcher) {
            val cache = createCache()
            val data = byteArrayOf(1, 2, 3)

            cache.put(zoomLvl = 1, col = 0, row = 0, providerId = "osm", data = data)

            // Remove file from disk but leave DB entries
            fakeFs.delete("osm/1/0/0.png")

            val result = cache.get(zoomLvl = 1, col = 0, row = 0, providerId = "osm")

            assertNull(result, "Should return null when file is missing from disk")
            // DB should be cleaned up
            assertNull(
                queries.selectCacheEntry(1L, 0L, 0L, "osm").executeAsOneOrNull(),
                "DB entry should be cleaned up when file is missing",
            )
        }

    @Test
    fun `CacheFileSystem interface has three required methods`() {
        // Compile-time verification that the interface has the expected methods.
        val fs: CacheFileSystem = fakeFs
        fs.read("test")
        fs.write("test", byteArrayOf())
        fs.delete("test")
    }
}
