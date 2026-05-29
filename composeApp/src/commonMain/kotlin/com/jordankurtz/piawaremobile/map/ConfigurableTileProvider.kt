package com.jordankurtz.piawaremobile.map

import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.map.cache.TileCache
import com.jordankurtz.piawaremobile.map.debug.TileCacheStatsTracker
import com.jordankurtz.piawaremobile.map.offline.OfflineTileStore
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import org.koin.core.annotation.Single
import ovh.plrapps.mapcompose.core.TileStreamProvider

@Single(binds = [TileStreamProvider::class])
class ConfigurableTileProvider(
    private val httpClient: HttpClient,
    private val tileCache: TileCache,
    private val configFlow: StateFlow<TileProviderConfig>,
    private val statsTracker: TileCacheStatsTracker,
    private val offlineTileStore: OfflineTileStore,
) : TileStreamProvider {
    override suspend fun getTileStream(
        row: Int,
        col: Int,
        zoomLvl: Int,
    ): RawSource? {
        tileCache.get(zoomLvl, col, row, configFlow.value.id)?.let { cached ->
            if (offlineTileStore.isPinned(
                    zoomLevel = zoomLvl,
                    col = col,
                    row = row,
                    providerId = configFlow.value.id,
                )
            ) {
                statsTracker.recordOfflineHit()
            } else {
                statsTracker.recordDiskHit()
            }
            return Buffer().apply { write(cached) }
        }

        val subdomain = if (configFlow.value.subdomains.isEmpty()) "" else configFlow.value.subdomains.random()
        val url = configFlow.value.buildUrl(zoom = zoomLvl, col = col, row = row, subdomain = subdomain)

        return try {
            val bytes = getStream(httpClient, url)
            tileCache.put(zoomLvl, col, row, configFlow.value.id, bytes)
            statsTracker.recordNetworkFetch()
            Buffer().apply { write(bytes) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Failed to load tile", e)
            statsTracker.recordError()
            null
        }
    }
}
