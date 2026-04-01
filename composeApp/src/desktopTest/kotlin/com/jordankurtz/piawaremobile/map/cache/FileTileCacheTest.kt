package com.jordankurtz.piawaremobile.map.cache

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FileTileCacheTest {
    private lateinit var cacheDir: File
    private lateinit var queries: TileCacheQueries
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        cacheDir = createTempDirectory("tile-cache-test").toFile()
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TileCacheDatabase.Schema.create(driver)
        queries = TileCacheDatabase(driver).tileCacheQueries
    }

    @AfterTest
    fun tearDown() {
        cacheDir.deleteRecursively()
    }

    private fun createCache(
        maxCacheBytes: Long = FileTileCache.DEFAULT_MAX_CACHE_BYTES,
        maxAgeMillis: Long = FileTileCache.DEFAULT_MAX_AGE_MILLIS,
        cacheScope: TestScope? = null,
    ): FileTileCache =
        FileTileCache(
            cacheFileSystem = JvmCacheFileSystem(cacheDir),
            queries = queries,
            ioDispatcher = testDispatcher,
            maxCacheBytes = maxCacheBytes,
            maxAgeMillis = maxAgeMillis,
            cacheScope = cacheScope,
        )

    @Test
    fun getReturnsNullForMissingTile() =
        runTest(testDispatcher) {
            val cache = createCache()

            val result = cache.get(zoomLvl = 1, col = 2, row = 3)

            assertNull(result)
        }

    @Test
    fun putThenGetReturnsSameData() =
        runTest(testDispatcher) {
            val cache = createCache()
            val data = byteArrayOf(1, 2, 3, 4, 5)

            cache.put(zoomLvl = 5, col = 10, row = 20, data = data)
            val result = cache.get(zoomLvl = 5, col = 10, row = 20)

            assertNotNull(result)
            assertContentEquals(data, result)
        }

    @Test
    fun differentTilesAreCachedSeparately() =
        runTest(testDispatcher) {
            val cache = createCache()
            val data1 = byteArrayOf(1, 2, 3)
            val data2 = byteArrayOf(4, 5, 6)

            cache.put(zoomLvl = 1, col = 0, row = 0, data = data1)
            cache.put(zoomLvl = 2, col = 0, row = 0, data = data2)

            assertContentEquals(data1, cache.get(zoomLvl = 1, col = 0, row = 0))
            assertContentEquals(data2, cache.get(zoomLvl = 2, col = 0, row = 0))
        }

    @Test
    fun expiredTileReturnsNull() =
        runTest(testDispatcher) {
            val cache = createCache(maxAgeMillis = 1L)
            val data = byteArrayOf(1, 2, 3)

            cache.put(zoomLvl = 1, col = 0, row = 0, data = data)

            // Backdate fetched_at to simulate expiration
            queries.upsertTile(
                1L,
                0L,
                0L,
                data.size.toLong(),
                kotlin.time.Clock.System.now().toEpochMilliseconds() - 1000,
            )

            val result = cache.get(zoomLvl = 1, col = 0, row = 0)
            assertNull(result)
            // Expired file should be deleted from disk
            val file = File(cacheDir, "1/0/0.png")
            assertTrue(!file.exists())
        }

    @Test
    fun evictsOldestFilesWhenCacheExceedsMaxSize() =
        runTest(testDispatcher) {
            val cache = createCache(maxCacheBytes = 10L, cacheScope = this)
            val data = byteArrayOf(1, 2, 3, 4, 5)

            cache.put(zoomLvl = 1, col = 0, row = 0, data = data)
            // Backdate access time so this tile gets evicted first
            queries.updateLastAccessed(
                kotlin.time.Clock.System.now().toEpochMilliseconds() - 5000,
                1L,
                0L,
                0L,
            )

            cache.put(zoomLvl = 1, col = 0, row = 1, data = data)

            // Third tile should trigger eviction of the oldest-accessed (row=0)
            cache.put(zoomLvl = 1, col = 0, row = 2, data = data)
            advanceUntilIdle()

            // The oldest-accessed tile (row=0) should have been evicted
            val evictedFile = File(cacheDir, "1/0/0.png")
            assertTrue(!evictedFile.exists(), "Oldest-accessed tile should be evicted")

            // Newer tiles should still be present
            assertNotNull(cache.get(zoomLvl = 1, col = 0, row = 2))
        }

    @Test
    fun createsDirectoriesAutomatically() =
        runTest(testDispatcher) {
            val nestedCacheDir = File(cacheDir, "nested/deep/cache")
            val cache =
                FileTileCache(
                    cacheFileSystem = JvmCacheFileSystem(nestedCacheDir),
                    queries = queries,
                    ioDispatcher = testDispatcher,
                )

            cache.put(zoomLvl = 5, col = 10, row = 20, data = byteArrayOf(1, 2, 3))

            assertTrue(File(nestedCacheDir, "5/10/20.png").exists())
        }

    @Test
    fun putOverwritesExistingTile() =
        runTest(testDispatcher) {
            val cache = createCache()
            val originalData = byteArrayOf(1, 2, 3)
            val updatedData = byteArrayOf(7, 8, 9, 10)

            cache.put(zoomLvl = 1, col = 0, row = 0, data = originalData)
            cache.put(zoomLvl = 1, col = 0, row = 0, data = updatedData)

            assertContentEquals(updatedData, cache.get(zoomLvl = 1, col = 0, row = 0))
        }

    @Test
    fun getReturnsEmptyArrayForZeroByteFile() =
        runTest(testDispatcher) {
            val cache = createCache()

            cache.put(zoomLvl = 1, col = 0, row = 0, data = byteArrayOf())
            val result = cache.get(zoomLvl = 1, col = 0, row = 0)

            assertNotNull(result)
            assertContentEquals(byteArrayOf(), result)
        }

    @Test
    fun recentlyAccessedTileSurvivesEviction() =
        runTest(testDispatcher) {
            val cache = createCache(maxCacheBytes = 10L, cacheScope = this)
            val data = byteArrayOf(1, 2, 3, 4, 5)

            // Put tile A (5 bytes)
            cache.put(zoomLvl = 1, col = 0, row = 0, data = data)
            queries.updateLastAccessed(
                kotlin.time.Clock.System.now().toEpochMilliseconds() - 10_000,
                1L,
                0L,
                0L,
            )

            // Put tile B (5 bytes) -- at 10 bytes, at limit
            cache.put(zoomLvl = 1, col = 0, row = 1, data = data)
            queries.updateLastAccessed(
                kotlin.time.Clock.System.now().toEpochMilliseconds() - 5_000,
                1L,
                0L,
                1L,
            )

            // Access tile A to make it recently used (updates last_accessed in DB)
            cache.get(zoomLvl = 1, col = 0, row = 0)

            // Put tile C -- exceeds limit, should evict tile B (the least recently accessed)
            cache.put(zoomLvl = 1, col = 0, row = 2, data = data)
            advanceUntilIdle()

            // Tile A should survive because it was recently accessed
            assertNotNull(
                cache.get(zoomLvl = 1, col = 0, row = 0),
                "Recently accessed tile should survive eviction",
            )
            // Tile B should be evicted
            val tileB = File(cacheDir, "1/0/1.png")
            assertTrue(!tileB.exists(), "Oldest-accessed tile should be evicted")
        }

    @Test
    fun emptyByteArrayCanBeCached() =
        runTest(testDispatcher) {
            val cache = createCache()
            val emptyData = byteArrayOf()

            cache.put(zoomLvl = 1, col = 0, row = 0, data = emptyData)
            val result = cache.get(zoomLvl = 1, col = 0, row = 0)

            assertNotNull(result)
            assertContentEquals(emptyData, result)
        }

    @Test
    fun expiredTileIsNotServedEvenIfRecentlyAccessed() =
        runTest(testDispatcher) {
            val cache = createCache(maxAgeMillis = 1L)
            val data = byteArrayOf(1, 2, 3)

            cache.put(zoomLvl = 1, col = 0, row = 0, data = data)

            // Backdate fetched_at to simulate expiration
            queries.upsertTile(
                1L,
                0L,
                0L,
                data.size.toLong(),
                kotlin.time.Clock.System.now().toEpochMilliseconds() - 1000,
            )

            val result = cache.get(zoomLvl = 1, col = 0, row = 0)
            assertNull(result, "Expired tile should not be served regardless of access time")
        }
}
