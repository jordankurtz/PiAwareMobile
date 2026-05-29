package com.jordankurtz.piawaremobile.map.cache

interface TileCache {
    suspend fun get(
        zoomLvl: Int,
        col: Int,
        row: Int,
        providerId: String,
    ): ByteArray?

    suspend fun put(
        zoomLvl: Int,
        col: Int,
        row: Int,
        providerId: String,
        data: ByteArray,
    )

    /**
     * Remove a tile from the cache (both disk and database metadata).
     * No-op if the tile is not cached.
     */
    suspend fun delete(
        zoomLvl: Int,
        col: Int,
        row: Int,
        providerId: String,
    )

    /** Delete all cached tiles that are not part of an offline region. */
    suspend fun clearAll()
}
