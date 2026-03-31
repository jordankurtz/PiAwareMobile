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
 * Disk-based tile cache that stores tile bytes on the filesystem and tracks
 * metadata (size, age, LRU order) in a SQLDelight database.
 *
 * Files are organized as `{cacheDir}/{zoom}/{col}/{row}.png`.
 * The database stores tile metadata in `tile` and cache membership in `cache_entry`,
 * enabling O(1) size lookups and O(log n) LRU eviction queries.
 *
 * @param cacheFileSystem Abstraction over raw file operations (read/write/delete only)
 * @param queries SQLDelight-generated queries for tile metadata
 * @param ioDispatcher Dispatcher for file I/O operations
 * @param maxCacheBytes Maximum total cache size in bytes (default 100 MB)
 * @param maxAgeMillis Maximum age of a cached tile in milliseconds (default 7 days)
 * @param cacheScope Scope for background eviction jobs; uses a [SupervisorJob] so that
 *   cancellation of individual tile workers does not cancel eviction
 */
class FileTileCache(
    private val cacheFileSystem: CacheFileSystem,
    private val queries: TileCacheQueries,
    private val ioDispatcher: CoroutineDispatcher,
    private val maxCacheBytes: Long = DEFAULT_MAX_CACHE_BYTES,
    private val maxAgeMillis: Long = DEFAULT_MAX_AGE_MILLIS,
    cacheScope: CoroutineScope? = null,
) : TileCache {
    private val scope = cacheScope ?: CoroutineScope(ioDispatcher + SupervisorJob())
    private val evictionMutex = Mutex()
    private var evictionScheduled = false

    override suspend fun get(
        zoomLvl: Int,
        col: Int,
        row: Int,
    ): ByteArray? =
        withContext(ioDispatcher) {
            val tileKey = tileKey(zoomLvl, col, row)
            val entry =
                queries.selectCacheEntry(
                    zoomLvl.toLong(),
                    col.toLong(),
                    row.toLong(),
                ).executeAsOneOrNull()

            if (entry == null) {
                Logger.d("Cache miss: $tileKey (not found)")
                return@withContext null
            }

            val age = Clock.System.now().toEpochMilliseconds() - entry.fetched_at
            if (age > maxAgeMillis) {
                Logger.d("Cache miss: $tileKey (expired)")
                cacheFileSystem.delete(tileKey)
                queries.deleteCacheEntry(zoomLvl.toLong(), col.toLong(), row.toLong())
                queries.deleteTile(zoomLvl.toLong(), col.toLong(), row.toLong())
                return@withContext null
            }

            val bytes =
                try {
                    cacheFileSystem.read(tileKey)
                } catch (e: Exception) {
                    Logger.e("Failed to read cached tile $zoomLvl/$col/$row", e)
                    null
                }
            if (bytes == null) {
                Logger.d("Cache miss: $tileKey (missing from disk)")
                queries.deleteCacheEntry(zoomLvl.toLong(), col.toLong(), row.toLong())
                queries.deleteTile(zoomLvl.toLong(), col.toLong(), row.toLong())
                return@withContext null
            }

            queries.updateLastAccessed(
                Clock.System.now().toEpochMilliseconds(),
                zoomLvl.toLong(),
                col.toLong(),
                row.toLong(),
            )
            Logger.d("Cache hit: $tileKey")
            bytes
        }

    override suspend fun put(
        zoomLvl: Int,
        col: Int,
        row: Int,
        data: ByteArray,
    ) {
        withContext(ioDispatcher) {
            val tileKey = tileKey(zoomLvl, col, row)
            try {
                cacheFileSystem.write(tileKey, data)
                val now = Clock.System.now().toEpochMilliseconds()
                queries.upsertTile(
                    zoomLvl.toLong(),
                    col.toLong(),
                    row.toLong(),
                    data.size.toLong(),
                    now,
                )
                queries.upsertCacheEntry(
                    zoomLvl.toLong(),
                    col.toLong(),
                    row.toLong(),
                    now,
                )
                Logger.d("Cached tile: $tileKey (${data.size} bytes)")
            } catch (e: Exception) {
                Logger.e("Failed to write cached tile $zoomLvl/$col/$row", e)
                return@withContext
            }

            val shouldSchedule =
                evictionMutex.withLock {
                    if (!evictionScheduled) {
                        evictionScheduled = true
                        true
                    } else {
                        false
                    }
                }
            if (shouldSchedule) {
                scope.launch { evictIfNeeded() }
            }
        }
    }

    /**
     * Evicts least-recently-accessed tiles until total cache size is at or below [maxCacheBytes].
     * Runs in [scope] so that cancellation of tile workers does not abort eviction.
     */
    private suspend fun evictIfNeeded() {
        evictionMutex.withLock {
            evictionScheduled = false
            val totalSize = queries.totalCacheSize().executeAsOne()
            if (totalSize <= maxCacheBytes) return

            val tiles = queries.selectLruTiles().executeAsList()
            var currentSize = totalSize
            for (tile in tiles) {
                if (currentSize <= maxCacheBytes) break
                val tileKey =
                    tileKey(
                        tile.zoom_level.toInt(),
                        tile.col.toInt(),
                        tile.row.toInt(),
                    )
                Logger.d(
                    "Evicting tile: $tileKey " +
                        "(cache at ${currentSize / 1024}KB / ${maxCacheBytes / 1024}KB)",
                )
                cacheFileSystem.delete(tileKey)
                queries.deleteCacheEntry(tile.zoom_level, tile.col, tile.row)
                queries.deleteTile(tile.zoom_level, tile.col, tile.row)
                currentSize -= tile.size_bytes
            }
        }
    }

    private fun tileKey(
        zoomLvl: Int,
        col: Int,
        row: Int,
    ): String = "$zoomLvl/$col/$row.png"

    companion object {
        const val DEFAULT_MAX_CACHE_BYTES = 100L * 1024 * 1024 // 100 MB
        const val DEFAULT_MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000 // 7 days
    }
}
