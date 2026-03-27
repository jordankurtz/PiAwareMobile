package com.jordankurtz.piawaremobile.map.cache

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

class FileTileCacheCommonTest {
    private lateinit var fakeFs: FakeCacheFileSystem
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        fakeFs = FakeCacheFileSystem()
    }

    private fun createCache(
        maxCacheBytes: Long = FileTileCache.DEFAULT_MAX_CACHE_BYTES,
        maxAgeMillis: Long = FileTileCache.DEFAULT_MAX_AGE_MILLIS,
    ): FileTileCache =
        FileTileCache(
            cacheFileSystem = fakeFs,
            ioDispatcher = testDispatcher,
            maxCacheBytes = maxCacheBytes,
            maxAgeMillis = maxAgeMillis,
        )

    @Test
    fun `get returns null for tile that has never been cached`() =
        runTest(testDispatcher) {
            val cache = createCache()

            val result = cache.get(zoomLvl = 1, col = 2, row = 3)

            assertNull(result)
        }

    @Test
    fun `put then get returns same ByteArray contents`() =
        runTest(testDispatcher) {
            val cache = createCache()
            val data = byteArrayOf(1, 2, 3, 4, 5)

            cache.put(zoomLvl = 5, col = 10, row = 20, data = data)
            val result = cache.get(zoomLvl = 5, col = 10, row = 20)

            assertNotNull(result)
            assertContentEquals(data, result)
        }

    @Test
    fun `tiles for different coordinates are stored and retrieved independently`() =
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
    fun `put overwrites existing tile data for same coordinates`() =
        runTest(testDispatcher) {
            val cache = createCache()
            val originalData = byteArrayOf(1, 2, 3)
            val updatedData = byteArrayOf(7, 8, 9, 10)

            cache.put(zoomLvl = 1, col = 0, row = 0, data = originalData)
            cache.put(zoomLvl = 1, col = 0, row = 0, data = updatedData)

            assertContentEquals(updatedData, cache.get(zoomLvl = 1, col = 0, row = 0))
        }

    @Test
    fun `expired tile returns null and deletes tile and access sidecar`() =
        runTest(testDispatcher) {
            val cache = createCache(maxAgeMillis = 1L)
            val data = byteArrayOf(1, 2, 3)

            cache.put(zoomLvl = 1, col = 0, row = 0, data = data)

            // Set the tile's modification time to the past to simulate expiration
            val pastTime = Clock.System.now().toEpochMilliseconds() - 1000
            fakeFs.setModifiedTime("1/0/0.png", pastTime)

            val result = cache.get(zoomLvl = 1, col = 0, row = 0)

            assertNull(result)
            assertFalse(fakeFs.exists("1/0/0.png"), "Expired tile file should be deleted")
            assertFalse(fakeFs.exists("1/0/0.access"), "Access sidecar should be deleted with expired tile")
        }

    @Test
    fun `get does not modify tile file modification date`() =
        runTest(testDispatcher) {
            val cache = createCache(maxAgeMillis = 60_000L)
            val data = byteArrayOf(1, 2, 3)

            cache.put(zoomLvl = 1, col = 0, row = 0, data = data)

            // Set tile mod time to a known value
            val knownTime = Clock.System.now().toEpochMilliseconds() - 5000
            fakeFs.setModifiedTime("1/0/0.png", knownTime)

            cache.get(zoomLvl = 1, col = 0, row = 0)

            assertTrue(
                fakeFs.getModifiedTime("1/0/0.png") == knownTime,
                "get() must not modify the tile file's lastModified (expiration clock)",
            )
        }

    @Test
    fun `get updates access sidecar modification date`() =
        runTest(testDispatcher) {
            val cache = createCache()
            val data = byteArrayOf(1, 2, 3)

            cache.put(zoomLvl = 1, col = 0, row = 0, data = data)

            // Set access sidecar to an old time
            val oldTime = Clock.System.now().toEpochMilliseconds() - 60_000
            fakeFs.setModifiedTime("1/0/0.access", oldTime)

            cache.get(zoomLvl = 1, col = 0, row = 0)

            assertTrue(
                fakeFs.getModifiedTime("1/0/0.access") > oldTime,
                "get() should update access sidecar's modification date for LRU tracking",
            )
        }

    @Test
    fun `evicts least recently accessed tiles when cache exceeds max size`() =
        runTest(testDispatcher) {
            val cache = createCache(maxCacheBytes = 10L)
            val data = byteArrayOf(1, 2, 3, 4, 5)

            cache.put(zoomLvl = 1, col = 0, row = 0, data = data)
            // Make first tile's access time old so it gets evicted first
            fakeFs.setModifiedTime("1/0/0.access", Clock.System.now().toEpochMilliseconds() - 5000)

            cache.put(zoomLvl = 1, col = 0, row = 1, data = data)

            // Third tile pushes cache to 15 bytes, exceeding 10-byte limit
            cache.put(zoomLvl = 1, col = 0, row = 2, data = data)

            // The oldest-accessed tile (row=0) should be evicted
            assertFalse(fakeFs.exists("1/0/0.png"), "Oldest-accessed tile should be evicted")

            // Newer tiles should remain
            assertNotNull(cache.get(zoomLvl = 1, col = 0, row = 2))
        }

    @Test
    fun `recently accessed tile survives eviction over less recently accessed tile`() =
        runTest(testDispatcher) {
            val cache = createCache(maxCacheBytes = 10L)
            val data = byteArrayOf(1, 2, 3, 4, 5)

            // Put tile A (5 bytes)
            cache.put(zoomLvl = 1, col = 0, row = 0, data = data)
            fakeFs.setModifiedTime("1/0/0.access", Clock.System.now().toEpochMilliseconds() - 10_000)

            // Put tile B (5 bytes) -- at 10 bytes, at limit
            cache.put(zoomLvl = 1, col = 0, row = 1, data = data)
            fakeFs.setModifiedTime("1/0/1.access", Clock.System.now().toEpochMilliseconds() - 5_000)

            // Access tile A to make it recently used
            cache.get(zoomLvl = 1, col = 0, row = 0)

            // Put tile C -- exceeds limit, should evict tile B (least recently accessed)
            cache.put(zoomLvl = 1, col = 0, row = 2, data = data)

            // Tile A should survive because it was recently accessed
            assertNotNull(
                cache.get(zoomLvl = 1, col = 0, row = 0),
                "Recently accessed tile should survive eviction",
            )
            // Tile B should be evicted
            assertFalse(fakeFs.exists("1/0/1.png"), "Least recently accessed tile should be evicted")
        }

    @Test
    fun `get returns null on file I-O error`() =
        runTest(testDispatcher) {
            val cache = createCache()
            val data = byteArrayOf(1, 2, 3)

            cache.put(zoomLvl = 1, col = 0, row = 0, data = data)

            // Enable read errors
            fakeFs.throwOnRead = true

            val result = cache.get(zoomLvl = 1, col = 0, row = 0)

            assertNull(result, "get() should return null on file I/O error")
        }

    @Test
    fun `put handles file I-O error without crashing`() =
        runTest(testDispatcher) {
            val cache = createCache()

            fakeFs.throwOnWrite = true

            // Should not throw
            cache.put(zoomLvl = 1, col = 0, row = 0, data = byteArrayOf(1, 2, 3))
        }

    @Test
    fun `get returns empty ByteArray for zero-byte cached file`() =
        runTest(testDispatcher) {
            val cache = createCache()

            cache.put(zoomLvl = 1, col = 0, row = 0, data = byteArrayOf())
            val result = cache.get(zoomLvl = 1, col = 0, row = 0)

            assertNotNull(result)
            assertContentEquals(byteArrayOf(), result)
        }

    @Test
    fun `eviction does not run when cache size is within limit`() =
        runTest(testDispatcher) {
            val cache = createCache(maxCacheBytes = 1000L)
            val data = byteArrayOf(1, 2, 3, 4, 5)

            cache.put(zoomLvl = 1, col = 0, row = 0, data = data)
            cache.put(zoomLvl = 1, col = 0, row = 1, data = data)

            // Both tiles should still exist (total 10 bytes, well under 1000 limit)
            assertTrue(fakeFs.exists("1/0/0.png"), "Tile should not be evicted when under limit")
            assertTrue(fakeFs.exists("1/0/1.png"), "Tile should not be evicted when under limit")
        }

    @Test
    fun `expired tile is not served even if frequently accessed`() =
        runTest(testDispatcher) {
            val cache = createCache(maxAgeMillis = 1L)
            val data = byteArrayOf(1, 2, 3)

            cache.put(zoomLvl = 1, col = 0, row = 0, data = data)

            // Set tile write time to the past so it is expired
            fakeFs.setModifiedTime("1/0/0.png", Clock.System.now().toEpochMilliseconds() - 1000)
            // Set access sidecar to recent time
            fakeFs.setModifiedTime("1/0/0.access", Clock.System.now().toEpochMilliseconds())

            val result = cache.get(zoomLvl = 1, col = 0, row = 0)

            assertNull(result, "Expired tile should not be served regardless of access time")
        }

    @Test
    fun `eviction deletes both png and access sidecar files`() =
        runTest(testDispatcher) {
            val cache = createCache(maxCacheBytes = 5L)
            val data = byteArrayOf(1, 2, 3, 4, 5)

            cache.put(zoomLvl = 1, col = 0, row = 0, data = data)
            fakeFs.setModifiedTime("1/0/0.access", Clock.System.now().toEpochMilliseconds() - 10_000)

            // Put second tile to trigger eviction of first
            cache.put(zoomLvl = 1, col = 0, row = 1, data = data)

            assertFalse(fakeFs.exists("1/0/0.png"), "Evicted tile file should be deleted")
            assertFalse(fakeFs.exists("1/0/0.access"), "Evicted access sidecar should be deleted")
        }

    @Test
    fun `CacheFileSystem interface has all seven required methods`() {
        // Compile-time verification that the interface has the expected methods.
        // If any method is missing, this test won't compile.
        val fs: CacheFileSystem = fakeFs
        fs.read("test")
        fs.write("test", byteArrayOf())
        fs.delete("test")
        fs.list()
        fs.lastModified("test")
        fs.setLastModified("test", 0L)
        fs.sizeBytes()
    }
}
