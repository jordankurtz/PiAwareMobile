package com.jordankurtz.piawaremobile.map.cache

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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
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
        cacheScope: TestScope? = null,
    ): FileTileCache =
        if (cacheScope != null) {
            FileTileCache(
                cacheFileSystem = JvmCacheFileSystem(cacheDir),
                ioDispatcher = testDispatcher,
                maxCacheBytes = maxCacheBytes,
                maxAgeMillis = maxAgeMillis,
                cacheScope = cacheScope,
            )
        } else {
            FileTileCache(
                cacheFileSystem = JvmCacheFileSystem(cacheDir),
                ioDispatcher = testDispatcher,
                maxCacheBytes = maxCacheBytes,
                maxAgeMillis = maxAgeMillis,
            )
        }

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
            val cache = createCache(maxCacheBytes = 10L, cacheScope = this)

            // Each tile is 5 bytes — two tiles = 10 bytes (at limit)
            val data = byteArrayOf(1, 2, 3, 4, 5)

            cache.put(zoomLvl = 1, col = 0, row = 0, data = data)
            // Make the access file older so this tile gets evicted first
            File(cacheDir, "1/0/0.access").setLastModified(System.currentTimeMillis() - 5000)

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

            // Simulate a corrupted/interrupted write: create an empty file
            val file = File(cacheDir, "1/0/0.png")
            file.parentFile?.mkdirs()
            file.createNewFile()

            val result = cache.get(zoomLvl = 1, col = 0, row = 0)

            // Empty file is still a valid cache entry (0 bytes), should return empty array
            assertNotNull(result)
            assertContentEquals(byteArrayOf(), result)
        }

    @Test
    fun getUpdatesAccessFileForLruTracking() =
        runTest(testDispatcher) {
            val cache = createCache()
            val data = byteArrayOf(1, 2, 3)

            cache.put(zoomLvl = 1, col = 0, row = 0, data = data)
            val accessFile = File(cacheDir, "1/0/0.access")

            // Set access file to a known old time
            val oldTime = System.currentTimeMillis() - 60_000
            accessFile.setLastModified(oldTime)

            // Reading should update the access file timestamp
            cache.get(zoomLvl = 1, col = 0, row = 0)

            assertTrue(
                accessFile.lastModified() > oldTime,
                "get() should update access file for LRU tracking",
            )
        }

    @Test
    fun recentlyAccessedTileSurvivesEviction() =
        runTest(testDispatcher) {
            val cache = createCache(maxCacheBytes = 10L, cacheScope = this)
            val data = byteArrayOf(1, 2, 3, 4, 5)

            // Put tile A (5 bytes)
            cache.put(zoomLvl = 1, col = 0, row = 0, data = data)
            // Make tile A's access time old
            File(cacheDir, "1/0/0.access").setLastModified(System.currentTimeMillis() - 10_000)

            // Put tile B (5 bytes) — now at 10 bytes, at limit
            cache.put(zoomLvl = 1, col = 0, row = 1, data = data)
            // Make tile B's access time old but more recent than A
            File(cacheDir, "1/0/1.access").setLastModified(System.currentTimeMillis() - 5_000)

            // Access tile A to make it recently used (updates .access file to now)
            cache.get(zoomLvl = 1, col = 0, row = 0)

            // Put tile C — exceeds limit, should evict tile B (the least recently accessed)
            cache.put(zoomLvl = 1, col = 0, row = 2, data = data)
            advanceUntilIdle()

            // Tile A should survive because it was recently accessed
            assertNotNull(
                cache.get(zoomLvl = 1, col = 0, row = 0),
                "Recently accessed tile should survive eviction",
            )
            // Tile B should be evicted (it was the least recently used)
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
    fun getDoesNotResetExpirationClock() =
        runTest(testDispatcher) {
            val cache = createCache(maxAgeMillis = 5000L)
            val data = byteArrayOf(1, 2, 3)

            cache.put(zoomLvl = 1, col = 0, row = 0, data = data)
            val tileFile = File(cacheDir, "1/0/0.png")

            // Set the tile's write time to 4 seconds ago (almost expired)
            val nearlyExpiredTime = System.currentTimeMillis() - 4000
            tileFile.setLastModified(nearlyExpiredTime)

            // Reading the tile should NOT reset the tile file's lastModified
            cache.get(zoomLvl = 1, col = 0, row = 0)

            assertTrue(
                tileFile.lastModified() <= nearlyExpiredTime,
                "get() must not reset the tile file's lastModified (expiration clock)",
            )
        }

    @Test
    fun fileSizeReturnsCorrectSizeForExistingFile() {
        val fs = JvmCacheFileSystem(cacheDir)
        fs.write("1/0/0.png", byteArrayOf(1, 2, 3, 4, 5))

        assertEquals(5L, fs.fileSize("1/0/0.png"))
    }

    @Test
    fun fileSizeReturnsZeroForNonexistentFile() {
        val fs = JvmCacheFileSystem(cacheDir)

        assertEquals(0L, fs.fileSize("nonexistent.png"))
    }

    @Test
    fun expiredTileIsNotServedEvenIfFrequentlyAccessed() =
        runTest(testDispatcher) {
            val cache = createCache(maxAgeMillis = 1L)
            val data = byteArrayOf(1, 2, 3)

            cache.put(zoomLvl = 1, col = 0, row = 0, data = data)

            // Set tile write time to the past so it is expired
            val tileFile = File(cacheDir, "1/0/0.png")
            tileFile.setLastModified(System.currentTimeMillis() - 1000)

            // Even though the access file is recent, the tile should be expired
            val result = cache.get(zoomLvl = 1, col = 0, row = 0)
            assertNull(result, "Expired tile should not be served regardless of access time")
        }
}
