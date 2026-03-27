package com.jordankurtz.piawaremobile.map.cache

import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSUUID
import platform.Foundation.stringByAppendingPathComponent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosCacheFileSystemTest {
    private lateinit var cacheDir: String
    private lateinit var fs: IosCacheFileSystem
    private val fileManager = NSFileManager.defaultManager

    @BeforeTest
    fun setUp() {
        val cachePaths = NSSearchPathForDirectoriesInDomains(
            NSCachesDirectory,
            NSUserDomainMask,
            true,
        )
        val baseCacheDir = cachePaths.first() as String
        @Suppress("CAST_NEVER_SUCCEEDS")
        cacheDir = (baseCacheDir as NSString).stringByAppendingPathComponent(
            "ios-cache-fs-test-${NSUUID().UUIDString}",
        )
        fs = IosCacheFileSystem(cacheDir)
    }

    @AfterTest
    fun tearDown() {
        fileManager.removeItemAtPath(cacheDir, error = null)
    }

    @Test
    fun readReturnsNullForNonexistentKey() {
        assertNull(fs.read("1/2/3.png"))
    }

    @Test
    fun writeAndReadRoundTrip() {
        val data = byteArrayOf(10, 20, 30, 40, 50)
        fs.write("5/10/20.png", data)

        val result = fs.read("5/10/20.png")
        assertNotNull(result)
        assertContentEquals(data, result)
    }

    @Test
    fun writeCreatesIntermediateDirectories() {
        fs.write("1/2/3.png", byteArrayOf(1))

        @Suppress("CAST_NEVER_SUCCEEDS")
        val dirPath = (cacheDir as NSString).stringByAppendingPathComponent("1/2")
        assertTrue(fileManager.fileExistsAtPath(dirPath))
    }

    @Test
    fun writeOverwritesExistingFile() {
        fs.write("1/0/0.png", byteArrayOf(1, 2, 3))
        fs.write("1/0/0.png", byteArrayOf(7, 8, 9, 10))

        val result = fs.read("1/0/0.png")
        assertNotNull(result)
        assertContentEquals(byteArrayOf(7, 8, 9, 10), result)
    }

    @Test
    fun deleteRemovesFile() {
        fs.write("1/0/0.png", byteArrayOf(1, 2, 3))
        fs.delete("1/0/0.png")

        assertNull(fs.read("1/0/0.png"))
    }

    @Test
    fun deleteNoOpForNonexistentFile() {
        // Should not throw
        fs.delete("nonexistent/file.png")
    }

    @Test
    fun listReturnsAllFiles() {
        fs.write("1/0/0.png", byteArrayOf(1))
        fs.write("1/0/0.access", byteArrayOf())
        fs.write("2/1/1.png", byteArrayOf(2))

        val keys = fs.list().sorted()
        assertEquals(3, keys.size)
        assertTrue(keys.contains("1/0/0.png"))
        assertTrue(keys.contains("1/0/0.access"))
        assertTrue(keys.contains("2/1/1.png"))
    }

    @Test
    fun listReturnsEmptyForNonexistentDir() {
        val emptyFs = IosCacheFileSystem("/nonexistent/path")
        assertEquals(emptyList(), emptyFs.list())
    }

    @Test
    fun lastModifiedReturnsMinusOneForNonexistentKey() {
        assertEquals(-1L, fs.lastModified("nope.png"))
    }

    @Test
    fun lastModifiedReturnsPositiveForExistingFile() {
        fs.write("1/0/0.png", byteArrayOf(1))
        val lastMod = fs.lastModified("1/0/0.png")
        assertTrue(lastMod > 0, "lastModified should be positive for existing file")
    }

    @Test
    fun setLastModifiedUpdatesTimestamp() {
        fs.write("1/0/0.png", byteArrayOf(1))
        val targetTime = 1000000000000L // a known epoch ms in the past
        fs.setLastModified("1/0/0.png", targetTime)

        val result = fs.lastModified("1/0/0.png")
        // Allow 1-second tolerance due to filesystem precision
        assertTrue(
            kotlin.math.abs(result - targetTime) < 1000,
            "setLastModified should update timestamp (expected ~$targetTime, got $result)",
        )
    }

    @Test
    fun setLastModifiedCreatesFileIfNotExists() {
        val now = 1700000000000L
        fs.setLastModified("1/0/0.access", now)

        val result = fs.lastModified("1/0/0.access")
        assertTrue(result > 0, "File should exist after setLastModified")
    }

    @Test
    fun sizeBytesCountsOnlyPngFiles() {
        fs.write("1/0/0.png", byteArrayOf(1, 2, 3, 4, 5))
        fs.write("1/0/0.access", byteArrayOf(0))
        fs.write("2/0/0.png", byteArrayOf(1, 2, 3))

        val size = fs.sizeBytes()
        // Should be 5 + 3 = 8 (excluding the .access file)
        assertEquals(8L, size)
    }

    @Test
    fun sizeBytesReturnsZeroForEmptyCache() {
        assertEquals(0L, fs.sizeBytes())
    }

    @Test
    fun readReturnsEmptyArrayForEmptyFile() {
        fs.write("1/0/0.png", byteArrayOf())
        val result = fs.read("1/0/0.png")
        assertNotNull(result)
        assertContentEquals(byteArrayOf(), result)
    }
}
