package com.jordankurtz.piawaremobile.map.offline

import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.cache.TileCache
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory
class OfflineDownloadEngine(
    private val tileCache: TileCache,
    private val offlineTileStore: OfflineTileStore,
    private val httpClient: HttpClient,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : DownloadEngine {
    /**
     * Downloads all tiles for [region] using [config] and pins them so they survive eviction.
     * Already-pinned tiles are skipped (safe to call on a partially-downloaded region).
     * Emits a [DownloadProgress] after each tile (downloaded or skipped).
     * Completes after all tiles are processed and region stats are updated.
     */
    override fun download(
        region: OfflineRegion,
        config: TileProviderConfig,
    ): Flow<DownloadProgress> =
        flow {
            try {
                withContext(ioDispatcher) {
                    offlineTileStore.updateDownloadStatus(region.id, DownloadStatus.DOWNLOADING, 0L)
                }

                val tiles =
                    tilesForRegion(
                        bounds =
                            BoundingBox(
                                minLat = region.minLat,
                                maxLat = region.maxLat,
                                minLon = region.minLon,
                                maxLon = region.maxLon,
                            ),
                        minZoom = region.minZoom,
                        maxZoom = region.maxZoom,
                    ).toList()
                val total = tiles.size.toLong()
                var downloaded = 0L
                var stored = 0L
                var totalBytes = 0L

                for (tile in tiles) {
                    val alreadyPinned =
                        withContext(ioDispatcher) {
                            offlineTileStore.isPinned(
                                zoomLevel = tile.zoom,
                                col = tile.col,
                                row = tile.row,
                            )
                        }

                    if (!alreadyPinned) {
                        val bytes =
                            fetchTile(
                                zoom = tile.zoom,
                                col = tile.col,
                                row = tile.row,
                                urlTemplate = config.urlTemplate,
                                userAgent = config.userAgent,
                            )
                        if (bytes != null) {
                            withContext(ioDispatcher) {
                                tileCache.put(
                                    zoomLvl = tile.zoom,
                                    col = tile.col,
                                    row = tile.row,
                                    data = bytes,
                                )
                                offlineTileStore.pinTile(
                                    zoomLevel = tile.zoom,
                                    col = tile.col,
                                    row = tile.row,
                                    regionId = region.id,
                                )
                            }
                            totalBytes += bytes.size
                            stored++
                            if (config.requestDelayMs > 0L) {
                                delay(config.requestDelayMs)
                            }
                        }
                    }

                    downloaded++
                    emit(DownloadProgress(regionId = region.id, downloaded = downloaded, total = total))
                }

                withContext(ioDispatcher) {
                    offlineTileStore.updateRegionStats(
                        id = region.id,
                        tileCount = stored,
                        sizeBytes = totalBytes,
                    )
                }
                withContext(ioDispatcher) {
                    offlineTileStore.updateDownloadStatus(region.id, DownloadStatus.COMPLETE, downloaded)
                }
            } catch (e: Exception) {
                Logger.e("Download failed for region ${region.id}", e)
                withContext(ioDispatcher) {
                    offlineTileStore.updateDownloadStatus(region.id, DownloadStatus.FAILED, 0L)
                }
                throw e
            }
        }

    private suspend fun fetchTile(
        zoom: Int,
        col: Int,
        row: Int,
        urlTemplate: String,
        userAgent: String,
    ): ByteArray? {
        val url =
            urlTemplate
                .replace("{z}", zoom.toString())
                .replace("{x}", col.toString())
                .replace("{y}", row.toString())
        return try {
            val response =
                httpClient.get(url) {
                    header("User-Agent", userAgent)
                }
            if (response.status.isSuccess()) response.body() else null
        } catch (e: Exception) {
            Logger.e("Failed to download tile $zoom/$col/$row", e)
            null
        }
    }
}
