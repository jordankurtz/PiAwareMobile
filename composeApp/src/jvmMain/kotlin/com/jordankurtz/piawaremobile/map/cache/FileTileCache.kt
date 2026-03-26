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
 * Expiration is based on the tile file's last-modified time, which represents when the
 * tile was originally fetched from the network. A separate `.access` sidecar file tracks
 * the most recent read time for LRU eviction ordering, so that accessing a tile does not
 * reset its expiration clock.
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
                accessFile(zoomLvl, col, row).delete()
                return@withContext null
            }

            try {
                val bytes = file.readBytes()
                // Touch the sidecar access file for LRU eviction tracking,
                // without modifying the tile file's lastModified (used for expiration)
                touchAccessFile(zoomLvl, col, row)
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
                // Set initial access time to match write time
                touchAccessFile(zoomLvl, col, row)
            } catch (e: Exception) {
                Logger.e("Failed to write cached tile $zoomLvl/$col/$row", e)
                return@withContext
            }

            evictIfNeeded()
        }
    }

    private suspend fun evictIfNeeded() {
        evictionMutex.withLock {
            val tileFiles =
                cacheDir.walkTopDown()
                    .filter { it.isFile && it.extension == "png" }
                    .toMutableList()

            val totalSize = tileFiles.sumOf { it.length() }
            if (totalSize <= maxCacheBytes) return

            // Sort by last access time ascending (least recently accessed first).
            // Use the .access sidecar if it exists, otherwise fall back to the tile's lastModified.
            tileFiles.sortBy { tile ->
                val access = accessFileForTile(tile)
                if (access.exists()) access.lastModified() else tile.lastModified()
            }

            var currentSize = totalSize
            for (file in tileFiles) {
                if (currentSize <= maxCacheBytes) break
                val fileSize = file.length()
                if (file.delete()) {
                    currentSize -= fileSize
                    // Also clean up the sidecar
                    accessFileForTile(file).delete()
                }
            }
        }
    }

    private fun tileFile(
        zoomLvl: Int,
        col: Int,
        row: Int,
    ): File = File(cacheDir, "$zoomLvl/$col/$row.png")

    private fun accessFile(
        zoomLvl: Int,
        col: Int,
        row: Int,
    ): File = File(cacheDir, "$zoomLvl/$col/$row.access")

    private fun accessFileForTile(tileFile: File): File {
        val path = tileFile.path.removeSuffix(".png") + ".access"
        return File(path)
    }

    private fun touchAccessFile(
        zoomLvl: Int,
        col: Int,
        row: Int,
    ) {
        val file = accessFile(zoomLvl, col, row)
        file.parentFile?.mkdirs()
        if (!file.exists()) {
            file.createNewFile()
        }
        file.setLastModified(System.currentTimeMillis())
    }

    companion object {
        const val DEFAULT_MAX_CACHE_BYTES = 100L * 1024 * 1024 // 100 MB
        const val DEFAULT_MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000 // 7 days
    }
}
