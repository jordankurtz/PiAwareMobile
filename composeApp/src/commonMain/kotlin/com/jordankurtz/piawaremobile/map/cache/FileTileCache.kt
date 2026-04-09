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
        providerId: String,
    ): ByteArray? =
        withContext(ioDispatcher) {
            val tileKey = tileKey(zoomLvl, col, row, providerId)
            val entry =
                queries.selectCacheEntry(
                    zoomLvl.toLong(),
                    col.toLong(),
                    row.toLong(),
                    providerId,
                ).executeAsOneOrNull()

            if (entry == null) {
                Logger.d("Cache miss: $tileKey (not found)")
                return@withContext null
            }

            val age = Clock.System.now().toEpochMilliseconds() - entry.fetched_at
            if (age > maxAgeMillis) {
                val pinned =
                    queries.isPinned(
                        zoom_level = zoomLvl.toLong(),
                        col = col.toLong(),
                        row = row.toLong(),
                        provider_id = providerId,
                    ).executeAsOne() > 0L
                if (pinned) {
                    Logger.d("Cache hit (pinned, skipping expiry check): $tileKey")
                    // fall through to serve the tile below
                } else {
                    Logger.d("Cache miss: $tileKey (expired)")
                    cacheFileSystem.delete(tileKey)
                    queries.deleteCacheEntry(zoomLvl.toLong(), col.toLong(), row.toLong(), providerId)
                    queries.deleteTile(zoomLvl.toLong(), col.toLong(), row.toLong(), providerId)
                    return@withContext null
                }
            }

            val bytes =
                try {
                    cacheFileSystem.read(tileKey)
                } catch (e: Exception) {
                    Logger.e("Failed to read cached tile $tileKey", e)
                    null
                }
            if (bytes == null) {
                Logger.d("Cache miss: $tileKey (missing from disk)")
                queries.deleteCacheEntry(zoomLvl.toLong(), col.toLong(), row.toLong(), providerId)
                queries.deleteTile(zoomLvl.toLong(), col.toLong(), row.toLong(), providerId)
                return@withContext null
            }

            queries.updateLastAccessed(
                Clock.System.now().toEpochMilliseconds(),
                zoomLvl.toLong(),
                col.toLong(),
                row.toLong(),
                providerId,
            )
            Logger.d("Cache hit: $tileKey")
            bytes
        }

    override suspend fun put(
        zoomLvl: Int,
        col: Int,
        row: Int,
        providerId: String,
        data: ByteArray,
    ) {
        withContext(ioDispatcher) {
            val tileKey = tileKey(zoomLvl, col, row, providerId)
            try {
                cacheFileSystem.write(tileKey, data)
                val now = Clock.System.now().toEpochMilliseconds()
                queries.upsertTile(
                    zoomLvl.toLong(),
                    col.toLong(),
                    row.toLong(),
                    providerId,
                    data.size.toLong(),
                    now,
                )
                queries.upsertCacheEntry(
                    zoomLvl.toLong(),
                    col.toLong(),
                    row.toLong(),
                    providerId,
                    now,
                )
                Logger.d("Cached tile: $tileKey (${data.size} bytes)")
            } catch (e: Exception) {
                Logger.e("Failed to write cached tile $tileKey", e)
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

    override suspend fun delete(
        zoomLvl: Int,
        col: Int,
        row: Int,
        providerId: String,
    ) {
        withContext(ioDispatcher) {
            val tileKey = tileKey(zoomLvl, col, row, providerId)
            cacheFileSystem.delete(tileKey)
            queries.deleteCacheEntry(zoomLvl.toLong(), col.toLong(), row.toLong(), providerId)
            queries.deleteTile(zoomLvl.toLong(), col.toLong(), row.toLong(), providerId)
        }
    }

    private suspend fun evictIfNeeded() {
        evictionMutex.withLock {
            evictionScheduled = false
            val totalSize = queries.totalCacheSize().executeAsOne()
            if (totalSize <= maxCacheBytes) return

            val excess = totalSize - maxCacheBytes
            val limit = ((excess / EVICTION_MIN_TILE_BYTES) * 1.2).toLong().coerceAtLeast(10L)
            val tiles = queries.selectLruTiles(limit).executeAsList()
            var currentSize = totalSize
            for (tile in tiles) {
                if (currentSize <= maxCacheBytes) break
                val tileKey =
                    tileKey(
                        tile.zoom_level.toInt(),
                        tile.col.toInt(),
                        tile.row.toInt(),
                        tile.provider_id,
                    )
                Logger.d(
                    "Evicting tile: $tileKey " +
                        "(cache at ${currentSize / 1024}KB / ${maxCacheBytes / 1024}KB)",
                )
                cacheFileSystem.delete(tileKey)
                queries.deleteCacheEntry(tile.zoom_level, tile.col, tile.row, tile.provider_id)
                queries.deleteTile(tile.zoom_level, tile.col, tile.row, tile.provider_id)
                currentSize -= tile.size_bytes
            }
        }
    }

    private fun tileKey(
        zoomLvl: Int,
        col: Int,
        row: Int,
        providerId: String,
    ): String = "$providerId/$zoomLvl/$col/$row.png"

    companion object {
        const val DEFAULT_MAX_CACHE_BYTES = 100L * 1024 * 1024
        const val DEFAULT_MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000
        private const val EVICTION_MIN_TILE_BYTES = 1024L
    }
}
