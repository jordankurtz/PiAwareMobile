package com.jordankurtz.piawaremobile.map.cache

/**
 * Abstraction for caching map tile data. Implementations may store tiles on disk,
 * in memory, or simply pass through (no-op).
 */
interface TileCache {
    /**
     * Retrieve cached tile bytes for the given tile coordinates, or null if not cached
     * or if the cached entry has expired.
     */
    suspend fun get(
        zoomLvl: Int,
        col: Int,
        row: Int,
    ): ByteArray?

    /**
     * Store tile bytes in the cache for the given tile coordinates.
     */
    suspend fun put(
        zoomLvl: Int,
        col: Int,
        row: Int,
        data: ByteArray,
    )
}
