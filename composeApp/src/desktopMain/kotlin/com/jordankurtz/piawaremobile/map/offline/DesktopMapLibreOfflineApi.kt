package com.jordankurtz.piawaremobile.map.offline

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class DesktopMapLibreOfflineApi : MapLibreOfflineApi {
    override suspend fun startDownload(
        styleUrl: String,
        bounds: BoundingBox,
        minZoom: Int,
        maxZoom: Int,
    ): Long = throw UnsupportedOperationException("Offline maps not supported on desktop")

    override fun observeProgress(nativeRegionId: Long): Flow<DownloadProgress> = emptyFlow()

    override suspend fun deleteRegion(nativeRegionId: Long): Unit =
        throw UnsupportedOperationException("Offline maps not supported on desktop")
}
