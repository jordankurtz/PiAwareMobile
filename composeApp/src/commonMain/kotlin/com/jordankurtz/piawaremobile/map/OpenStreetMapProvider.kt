package com.jordankurtz.piawaremobile.map

import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.map.cache.TileCache
import io.ktor.client.HttpClient
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.readByteArray
import org.koin.core.annotation.Single
import ovh.plrapps.mapcompose.core.TileStreamProvider

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
        // Check cache first
        tileCache.get(zoomLvl, col, row)?.let { cached ->
            return Buffer().apply { write(cached) }
        }

        // Cache miss — fetch from network
        return try {
            val source = getStream(httpClient, "https://tile.openstreetmap.org/$zoomLvl/$col/$row.png")
            val buffer = Buffer()
            source.use { raw ->
                while (raw.readAtMostTo(buffer, Long.MAX_VALUE) != -1L) {
                    // read until exhausted
                }
            }
            val bytes = buffer.readByteArray()

            // Store in cache (failures are logged inside TileCache)
            tileCache.put(zoomLvl, col, row, bytes)

            Buffer().apply { write(bytes) }
        } catch (e: Exception) {
            Logger.e("Failed to load tile", e)
            null
        }
    }
}
