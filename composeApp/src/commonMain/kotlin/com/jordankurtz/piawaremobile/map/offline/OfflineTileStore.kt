package com.jordankurtz.piawaremobile.map.offline

interface OfflineTileStore {
    suspend fun saveRegion(region: OfflineRegion): Long

    suspend fun getRegions(): List<OfflineRegion>

    suspend fun getRegion(id: Long): OfflineRegion?

    suspend fun deleteRegion(id: Long)

    suspend fun updateRegionStats(
        id: Long,
        tileCount: Long,
        sizeBytes: Long,
    )

    suspend fun updateDownloadStatus(
        id: Long,
        status: DownloadStatus,
        downloadedTileCount: Long,
    )

    suspend fun resetStuckDownloads()

    /**
     * Returns the bytes that would be freed if [id] is deleted
     * (tiles exclusively pinned to this region, not shared with others).
     */
    suspend fun getFreedBytesForRegion(id: Long): Long

    /**
     * Returns (zoom, col, row) triples for tiles exclusively pinned to [id].
     * These can be safely removed from the tile cache when the region is deleted.
     */
    suspend fun getExclusiveTilesForRegion(id: Long): List<Triple<Int, Int, Int>>

    suspend fun pinTile(
        zoomLevel: Int,
        col: Int,
        row: Int,
        regionId: Long,
        providerId: String,
    )

    suspend fun isPinned(
        zoomLevel: Int,
        col: Int,
        row: Int,
        providerId: String,
    ): Boolean

    suspend fun getPinnedTilesForRegion(regionId: Long): List<Triple<Int, Int, Int>>
}
