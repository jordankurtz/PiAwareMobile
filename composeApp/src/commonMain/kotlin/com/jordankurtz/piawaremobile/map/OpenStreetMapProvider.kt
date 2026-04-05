package com.jordankurtz.piawaremobile.map

import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.map.cache.TileCache
import com.jordankurtz.piawaremobile.map.debug.TileCacheStatsTracker
import com.jordankurtz.piawaremobile.map.offline.OfflineTileStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.prepareGet
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.readByteArray
import org.koin.core.annotation.Single
import ovh.plrapps.mapcompose.core.TileStreamProvider
import kotlin.time.TimeSource

@Single(binds = [TileStreamProvider::class])
class OpenStreetMapProvider(
    private val httpClient: HttpClient,
    private val tileCache: TileCache,
    private val offlineTileStore: OfflineTileStore,
    private val statsTracker: TileCacheStatsTracker,
) : TileStreamProvider {
    override suspend fun getTileStream(
        row: Int,
        col: Int,
        zoomLvl: Int,
    ): RawSource? {
        Logger.d("Loading tile z=$zoomLvl x=$col y=$row")
        val mark = TimeSource.Monotonic.markNow()

        // Check cache first
        tileCache.get(zoomLvl, col, row)?.let { cached ->
            val elapsed = mark.elapsedNow().inWholeMilliseconds
            Logger.d("Tile cache hit z=$zoomLvl x=$col y=$row (${elapsed}ms)")
            if (offlineTileStore.isPinned(zoomLevel = zoomLvl, col = col, row = row)) {
                statsTracker.recordOfflineHit()
            } else {
                statsTracker.recordDiskHit()
            }
            return Buffer().apply { write(cached) }
        }

        // Cache miss — fetch from network
        Logger.d("Tile cache miss z=$zoomLvl x=$col y=$row, fetching from network")
        val networkMark = TimeSource.Monotonic.markNow()
        val url = "https://tile.openstreetmap.org/$zoomLvl/$col/$row.png"
        return try {
            val bytes = fetchTileBytes(url)
            if (bytes == null) {
                val networkElapsed = networkMark.elapsedNow().inWholeMilliseconds
                Logger.w("Non-success HTTP response for tile z=$zoomLvl x=$col y=$row (${networkElapsed}ms)")
                statsTracker.recordError()
                return null
            }
            val networkElapsed = networkMark.elapsedNow().inWholeMilliseconds
            Logger.d("Tile loaded from network z=$zoomLvl x=$col y=$row (${networkElapsed}ms)")

            // Store in cache (failures are logged inside TileCache)
            tileCache.put(zoomLvl, col, row, bytes)
            statsTracker.recordNetworkFetch()

            Buffer().apply { write(bytes) }
        } catch (e: Exception) {
            val networkElapsed = networkMark.elapsedNow().inWholeMilliseconds
            Logger.e("Failed to load tile z=$zoomLvl x=$col y=$row (${networkElapsed}ms)", e)
            statsTracker.recordError()
            null
        }
    }

    /**
     * Fetches tile bytes from the given URL. Returns null if the HTTP response status
     * is not successful (not 2xx), ensuring error responses are never cached.
     */
    private suspend fun fetchTileBytes(url: String): ByteArray? {
        val buffer = Buffer()
        var success = false
        httpClient.prepareGet(url).execute { httpResponse ->
            if (!httpResponse.status.isSuccess()) {
                return@execute
            }
            success = true
            val channel: ByteReadChannel = httpResponse.body()
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                while (!packet.exhausted()) {
                    val bytes = packet.readByteArray()
                    buffer.write(bytes, 0, bytes.size)
                }
            }
        }
        return if (success) buffer.readByteArray() else null
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 8192
    }
}
