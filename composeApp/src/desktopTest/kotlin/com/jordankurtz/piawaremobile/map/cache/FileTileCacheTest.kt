package com.jordankurtz.piawaremobile.map.cache

import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileTileCacheTest {
    private lateinit var cacheDir: File
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        cacheDir = createTempDirectory("tile-cache-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        cacheDir.deleteRecursively()
    }

    private fun createCache(
        maxCacheBytes: Long = FileTileCache.DEFAULT_MAX_CACHE_BYTES,
        maxAgeMillis: Long = FileTileCache.DEFAULT_MAX_AGE_MILLIS,
    ): FileTileCache =
        FileTileCache(
            cacheDir = cacheDir,
            ioDispatcher = testDispatcher,
            maxCacheBytes = maxCacheBytes,
            maxAgeMillis = maxAgeMillis,
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

            // Set the file's last modified time to the past to simulate expiration
            val file = File(cacheDir, "1/0/0.png")
            assertTrue(file.exists())
            file.setLastModified(System.currentTimeMillis() - 1000)

            val result = cache.get(zoomLvl = 1, col = 0, row = 0)
            assertNull(result)
            // Expired file should be deleted
            assertTrue(!file.exists())
        }

    @Test
    fun evictsOldestFilesWhenCacheExceedsMaxSize() =
        runTest(testDispatcher) {
            // Set a very small max size to force eviction
            val cache = createCache(maxCacheBytes = 10L)

            // Each tile is 5 bytes — two tiles = 10 bytes (at limit)
            val data = byteArrayOf(1, 2, 3, 4, 5)

            cache.put(zoomLvl = 1, col = 0, row = 0, data = data)
            // Touch with older timestamp
            File(cacheDir, "1/0/0.png").setLastModified(System.currentTimeMillis() - 5000)

            cache.put(zoomLvl = 1, col = 0, row = 1, data = data)

            // Third tile should trigger eviction of the oldest (row=0)
            cache.put(zoomLvl = 1, col = 0, row = 2, data = data)

            // The oldest tile (row=0) should have been evicted
            val evictedFile = File(cacheDir, "1/0/0.png")
            assertTrue(!evictedFile.exists(), "Oldest tile should be evicted")

            // Newer tiles should still be present
            assertNotNull(cache.get(zoomLvl = 1, col = 0, row = 2))
        }

    @Test
    fun createsDirectoriesAutomatically() =
        runTest(testDispatcher) {
            val nestedCacheDir = File(cacheDir, "nested/deep/cache")
            val cache =
                FileTileCache(
                    cacheDir = nestedCacheDir,
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
}
