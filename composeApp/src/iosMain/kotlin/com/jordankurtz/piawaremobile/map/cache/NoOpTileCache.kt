package com.jordankurtz.piawaremobile.map.cache

/**
 * A no-op [TileCache] for iOS where `java.io.File` is not available.
 * All get calls return null (cache miss) and put calls are ignored.
 */
class NoOpTileCache : TileCache {
    override suspend fun get(
        zoomLvl: Int,
        col: Int,
        row: Int,
    ): ByteArray? = null

    override suspend fun put(
        zoomLvl: Int,
        col: Int,
        row: Int,
        data: ByteArray,
    ) {
        // No-op: iOS does not support disk tile caching in this implementation
    }
}
