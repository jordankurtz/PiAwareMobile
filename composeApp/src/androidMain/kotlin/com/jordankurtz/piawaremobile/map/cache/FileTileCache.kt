package com.jordankurtz.piawaremobile.map.cache

import com.jordankurtz.logger.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Disk-based tile cache that stores tiles as individual files under a cache directory.
 * Files are organized as `{cacheDir}/{zoom}/{col}/{row}.png`.
 *
 * @param cacheDir Root directory for cached tiles
 * @param ioDispatcher Dispatcher for file I/O operations
 * @param maxCacheBytes Maximum total cache size in bytes (default 100 MB)
 * @param maxAgeMillis Maximum age of a cached tile in milliseconds (default 7 days)
 */
class FileTileCache(
    private val cacheDir: File,
    private val ioDispatcher: CoroutineDispatcher,
    private val maxCacheBytes: Long = DEFAULT_MAX_CACHE_BYTES,
    private val maxAgeMillis: Long = DEFAULT_MAX_AGE_MILLIS,
) : TileCache {
    private val evictionMutex = Mutex()

    override suspend fun get(
        zoomLvl: Int,
        col: Int,
        row: Int,
    ): ByteArray? =
        withContext(ioDispatcher) {
            val file = tileFile(zoomLvl, col, row)
            if (!file.exists()) return@withContext null

            val age = System.currentTimeMillis() - file.lastModified()
            if (age > maxAgeMillis) {
                file.delete()
                return@withContext null
            }

            try {
                val bytes = file.readBytes()
                // Touch the file to update access time for LRU eviction
                file.setLastModified(System.currentTimeMillis())
                bytes
            } catch (e: Exception) {
                Logger.e("Failed to read cached tile $zoomLvl/$col/$row", e)
                null
            }
        }

    override suspend fun put(
        zoomLvl: Int,
        col: Int,
        row: Int,
        data: ByteArray,
    ) {
        withContext(ioDispatcher) {
            try {
                val file = tileFile(zoomLvl, col, row)
                file.parentFile?.mkdirs()
                file.writeBytes(data)
            } catch (e: Exception) {
                Logger.e("Failed to write cached tile $zoomLvl/$col/$row", e)
                return@withContext
            }

            evictIfNeeded()
        }
    }

    private suspend fun evictIfNeeded() {
        evictionMutex.withLock {
            val files =
                cacheDir.walkTopDown()
                    .filter { it.isFile }
                    .toMutableList()

            val totalSize = files.sumOf { it.length() }
            if (totalSize <= maxCacheBytes) return

            // Sort by lastModified ascending (oldest first) for LRU eviction
            files.sortBy { it.lastModified() }

            var currentSize = totalSize
            for (file in files) {
                if (currentSize <= maxCacheBytes) break
                val fileSize = file.length()
                if (file.delete()) {
                    currentSize -= fileSize
                }
            }
        }
    }

    private fun tileFile(
        zoomLvl: Int,
        col: Int,
        row: Int,
    ): File = File(cacheDir, "$zoomLvl/$col/$row.png")

    companion object {
        const val DEFAULT_MAX_CACHE_BYTES = 100L * 1024 * 1024 // 100 MB
        const val DEFAULT_MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000 // 7 days
    }
}
