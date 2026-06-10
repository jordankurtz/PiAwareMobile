package com.jordankurtz.piawaremobile.map.offline

import kotlinx.coroutines.flow.Flow

interface MapLibreOfflineApi {
    /**
     * Starts downloading all tiles for [styleUrl] within [bounds] at zoom levels
     * [minZoom]..[maxZoom]. Returns the native region ID assigned by MapLibre.
     */
    suspend fun startDownload(
        styleUrl: String,
        bounds: BoundingBox,
        minZoom: Int,
        maxZoom: Int,
    ): Long

    /**
     * Emits [DownloadProgress] updates for the region identified by [nativeRegionId].
     * Completes when the download finishes. Throws on error.
     */
    fun observeProgress(nativeRegionId: Long): Flow<DownloadProgress>

    /**
     * Removes the offline region from MapLibre's internal database.
     */
    suspend fun deleteRegion(nativeRegionId: Long)
}
