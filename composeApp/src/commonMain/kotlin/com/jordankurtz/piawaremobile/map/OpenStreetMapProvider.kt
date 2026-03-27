package com.jordankurtz.piawaremobile.map

import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.map.cache.TileCache
import io.ktor.client.HttpClient
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
            return Buffer().apply { write(cached) }
        }

        // Cache miss — fetch from network
        Logger.d("Tile cache miss z=$zoomLvl x=$col y=$row, fetching from network")
        val networkMark = TimeSource.Monotonic.markNow()
        return try {
            val source = getStream(httpClient, "https://tile.openstreetmap.org/$zoomLvl/$col/$row.png")
            val buffer = Buffer()
            source.use { raw ->
                while (raw.readAtMostTo(buffer, Long.MAX_VALUE) != -1L) {
                    // read until exhausted
                }
            }
            val bytes = buffer.readByteArray()
            val networkElapsed = networkMark.elapsedNow().inWholeMilliseconds
            Logger.d("Tile loaded from network z=$zoomLvl x=$col y=$row (${networkElapsed}ms)")

            // Store in cache (failures are logged inside TileCache)
            tileCache.put(zoomLvl, col, row, bytes)

            Buffer().apply { write(bytes) }
        } catch (e: Exception) {
            val networkElapsed = networkMark.elapsedNow().inWholeMilliseconds
            Logger.e("Failed to load tile z=$zoomLvl x=$col y=$row (${networkElapsed}ms)", e)
            null
        }
    }
}
