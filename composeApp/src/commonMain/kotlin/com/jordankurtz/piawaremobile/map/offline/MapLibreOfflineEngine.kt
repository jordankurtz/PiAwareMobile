package com.jordankurtz.piawaremobile.map.offline

import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.map.TileProviderConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.core.annotation.Factory

@Factory(binds = [DownloadEngine::class])
class MapLibreOfflineEngine(
    private val api: MapLibreOfflineApi,
    private val offlineTileStore: OfflineTileStore,
) : DownloadEngine {
    override fun download(
        region: OfflineRegion,
        config: TileProviderConfig,
    ): Flow<DownloadProgress> =
        flow {
            val nativeId =
                api.startDownload(
                    styleUrl = config.styleUrl,
                    bounds =
                        BoundingBox(
                            minLat = region.minLat,
                            maxLat = region.maxLat,
                            minLon = region.minLon,
                            maxLon = region.maxLon,
                        ),
                    minZoom = region.minZoom,
                    maxZoom = region.maxZoom,
                )
            offlineTileStore.setNativeRegionId(region.id, nativeId)
            var lastDownloaded = 0L
            var lastTotal = 0L
            try {
                api.observeProgress(nativeId).collect { progress ->
                    lastDownloaded = progress.downloaded
                    lastTotal = progress.total
                    emit(progress)
                }
                offlineTileStore.updateRegionStats(
                    id = region.id,
                    tileCount = lastTotal,
                    sizeBytes = 0L,
                )
                offlineTileStore.updateDownloadStatus(region.id, DownloadStatus.COMPLETE, lastDownloaded)
            } catch (e: Exception) {
                Logger.e("MapLibre offline download failed for region ${region.id}", e)
                offlineTileStore.updateDownloadStatus(region.id, DownloadStatus.FAILED, lastDownloaded)
                throw e
            }
        }
}
