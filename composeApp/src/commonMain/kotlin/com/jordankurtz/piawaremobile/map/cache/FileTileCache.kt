package com.jordankurtz.piawaremobile.map.cache

import com.jordankurtz.logger.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * Disk-based tile cache that stores tiles as individual files under a cache directory.
 * Files are organized as `{cacheDir}/{zoom}/{col}/{row}.png`.
 *
 * Expiration is based on the tile file's last-modified time, which represents when the
 * tile was originally fetched from the network. A separate `.access` sidecar file tracks
 * the most recent read time for LRU eviction ordering, so that accessing a tile does not
 * reset its expiration clock.
 *
 * All file I/O is delegated to [cacheFileSystem], keeping this class platform-independent.
 *
 * @param cacheFileSystem Abstraction over raw file operations
 * @param ioDispatcher Dispatcher for file I/O operations
 * @param maxCacheBytes Maximum total cache size in bytes (default 100 MB)
 * @param maxAgeMillis Maximum age of a cached tile in milliseconds (default 7 days)
 * @param cacheScope Scope for background eviction jobs; uses a [SupervisorJob] so that
 *   cancellation of individual tile workers does not cancel eviction
 */
class FileTileCache(
    private val cacheFileSystem: CacheFileSystem,
    private val ioDispatcher: CoroutineDispatcher,
    private val maxCacheBytes: Long = DEFAULT_MAX_CACHE_BYTES,
    private val maxAgeMillis: Long = DEFAULT_MAX_AGE_MILLIS,
    private val cacheScope: CoroutineScope = CoroutineScope(ioDispatcher + SupervisorJob()),
) : TileCache {
    private val evictionMutex = Mutex()
    private var totalSizeBytes: Long = UNINITIALIZED_SIZE
    private var evictionScheduled = false

    override suspend fun get(
        zoomLvl: Int,
        col: Int,
        row: Int,
    ): ByteArray? =
        withContext(ioDispatcher) {
            val tileKey = tileKey(zoomLvl, col, row)
            val accessKey = accessKey(zoomLvl, col, row)

            val lastMod = cacheFileSystem.lastModified(tileKey)
            if (lastMod == -1L) {
                Logger.d("Cache miss: $tileKey (not found)")
                return@withContext null
            }

            val age = Clock.System.now().toEpochMilliseconds() - lastMod
            if (age > maxAgeMillis) {
                Logger.d("Cache miss: $tileKey (expired)")
                cacheFileSystem.delete(tileKey)
                cacheFileSystem.delete(accessKey)
                return@withContext null
            }

            try {
                val bytes =
                    cacheFileSystem.read(tileKey) ?: run {
                        Logger.d("Cache miss: $tileKey (not found)")
                        return@withContext null
                    }
                // Touch the sidecar access file for LRU eviction tracking,
                // without modifying the tile file's lastModified (used for expiration)
                cacheFileSystem.setLastModified(
                    accessKey,
                    Clock.System.now().toEpochMilliseconds(),
                )
                Logger.d("Cache hit: $tileKey")
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
            val sizeChange: Long
            try {
                val tileKey = tileKey(zoomLvl, col, row)
                val accessKey = accessKey(zoomLvl, col, row)
                val previousSize = cacheFileSystem.fileSize(tileKey)
                cacheFileSystem.write(tileKey, data)
                sizeChange = data.size.toLong() - previousSize
                // Set initial access time to match write time
                cacheFileSystem.setLastModified(
                    accessKey,
                    Clock.System.now().toEpochMilliseconds(),
                )
                Logger.d("Cached tile: $tileKey (${data.size} bytes)")
            } catch (e: Exception) {
                Logger.e("Failed to write cached tile $zoomLvl/$col/$row", e)
                return@withContext
            }

            // Fast O(1) size update under mutex — never blocks on filesystem I/O
            val shouldSchedule =
                evictionMutex.withLock {
                    if (totalSizeBytes == UNINITIALIZED_SIZE) {
                        // First put: read the actual FS size which already
                        // includes the tile that was just written, so skip sizeChange.
                        totalSizeBytes = cacheFileSystem.sizeBytes()
                    } else {
                        totalSizeBytes += sizeChange
                    }
                    if (totalSizeBytes > maxCacheBytes && !evictionScheduled) {
                        evictionScheduled = true
                        true
                    } else {
                        false
                    }
                }
            if (shouldSchedule) {
                cacheScope.launch { evictIfNeeded() }
            }
        }
    }

    /**
     * Evicts least-recently-accessed tiles until [totalSizeBytes] is at or below [maxCacheBytes].
     * Runs in [cacheScope] so that cancellation of tile workers does not abort eviction.
     */
    private suspend fun evictIfNeeded() {
        evictionMutex.withLock {
            evictionScheduled = false
            if (totalSizeBytes <= maxCacheBytes) return

            // Collect all .png tile keys with their access times for LRU sorting
            val tileKeys =
                cacheFileSystem.list()
                    .filter { it.endsWith(".png") }
                    .toMutableList()

            // Sort by last access time ascending (least recently accessed first).
            // Use the .access sidecar if it exists, otherwise fall back to the tile's lastModified.
            tileKeys.sortBy { key ->
                val accessKey = key.removeSuffix(".png") + ".access"
                val accessTime = cacheFileSystem.lastModified(accessKey)
                if (accessTime != -1L) accessTime else cacheFileSystem.lastModified(key)
            }

            var currentSize = totalSizeBytes
            for (key in tileKeys) {
                if (currentSize <= maxCacheBytes) break
                val tileSize = cacheFileSystem.fileSize(key)
                Logger.d(
                    "Evicting tile: $key (cache at ${currentSize / 1024}KB / ${maxCacheBytes / 1024}KB)",
                )
                val accessKey = key.removeSuffix(".png") + ".access"
                cacheFileSystem.delete(key)
                cacheFileSystem.delete(accessKey)
                currentSize -= tileSize
            }
            totalSizeBytes = currentSize
        }
    }

    private fun tileKey(
        zoomLvl: Int,
        col: Int,
        row: Int,
    ): String = "$zoomLvl/$col/$row.png"

    private fun accessKey(
        zoomLvl: Int,
        col: Int,
        row: Int,
    ): String = "$zoomLvl/$col/$row.access"

    companion object {
        const val DEFAULT_MAX_CACHE_BYTES = 100L * 1024 * 1024 // 100 MB
        const val DEFAULT_MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000 // 7 days
        private const val UNINITIALIZED_SIZE = -1L
    }
}
