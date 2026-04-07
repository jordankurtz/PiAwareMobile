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

    suspend fun pinTile(
        zoomLevel: Int,
        col: Int,
        row: Int,
        regionId: Long,
    )

    suspend fun isPinned(
        zoomLevel: Int,
        col: Int,
        row: Int,
    ): Boolean

    suspend fun getPinnedTilesForRegion(regionId: Long): List<TileCoord>

    suspend fun updateDownloadStatus(
        id: Long,
        status: DownloadStatus,
        downloadedTileCount: Long = 0L,
    )

    suspend fun getExclusiveTilesForRegion(id: Long): List<TileCoord>

    suspend fun getFreedBytesForRegion(id: Long): Long

    suspend fun resetStuckDownloads()
}
