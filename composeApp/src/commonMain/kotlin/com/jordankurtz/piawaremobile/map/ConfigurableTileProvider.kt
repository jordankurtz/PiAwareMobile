package com.jordankurtz.piawaremobile.map

import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.map.cache.TileCache
import com.jordankurtz.piawaremobile.map.debug.TileCacheStatsTracker
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.StateFlow
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.readByteArray
import org.koin.core.annotation.Single
import ovh.plrapps.mapcompose.core.TileStreamProvider

@Single(binds = [TileStreamProvider::class])
class ConfigurableTileProvider(
    private val httpClient: HttpClient,
    private val tileCache: TileCache,
    private val configFlow: StateFlow<TileProviderConfig>,
    private val statsTracker: TileCacheStatsTracker,
) : TileStreamProvider {
    override suspend fun getTileStream(
        row: Int,
        col: Int,
        zoomLvl: Int,
    ): RawSource? {
        tileCache.get(zoomLvl, col, row, configFlow.value.id)?.let { cached ->
            statsTracker.recordDiskHit()
            return Buffer().apply { write(cached) }
        }

        val subdomain = if (configFlow.value.subdomains.isEmpty()) "" else configFlow.value.subdomains.random()
        val url = configFlow.value.buildUrl(zoom = zoomLvl, col = col, row = row, subdomain = subdomain)

        return try {
            val source = getStream(httpClient, url)
            val buffer = Buffer()
            source.use { raw ->
                while (raw.readAtMostTo(buffer, Long.MAX_VALUE) != -1L) {
                    // read until exhausted
                }
            }
            val bytes = buffer.readByteArray()
            tileCache.put(zoomLvl, col, row, configFlow.value.id, bytes)
            statsTracker.recordNetworkFetch()
            Buffer().apply { write(bytes) }
        } catch (e: Exception) {
            Logger.e("Failed to load tile", e)
            statsTracker.recordError()
            null
        }
    }
}
