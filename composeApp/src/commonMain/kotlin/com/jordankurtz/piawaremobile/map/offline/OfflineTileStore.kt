package com.jordankurtz.piawaremobile.map.offline

interface OfflineTileStore {
    suspend fun saveRegion(region: OfflineRegion): Long

    suspend fun getRegions(): List<OfflineRegion>

    suspend fun getRegion(id: Long): OfflineRegion?

    suspend fun deleteRegion(id: Long)

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
