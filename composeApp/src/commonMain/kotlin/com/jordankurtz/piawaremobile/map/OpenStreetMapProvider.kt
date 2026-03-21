package com.jordankurtz.piawaremobile.map

import com.jordankurtz.logger.Logger
import io.ktor.client.HttpClient
import kotlinx.io.RawSource
import org.koin.core.annotation.Single
import ovh.plrapps.mapcompose.core.TileStreamProvider

@Single(binds = [TileStreamProvider::class])
class OpenStreetMapProvider(private val httpClient: HttpClient) : TileStreamProvider {
    var useDarkTiles: Boolean = false

    override suspend fun getTileStream(
        row: Int,
        col: Int,
        zoomLvl: Int,
    ): RawSource? {
        val baseUrl =
            if (useDarkTiles) {
                "https://basemaps.cartocdn.com/dark_all/$zoomLvl/$col/$row.png"
            } else {
                "https://tile.openstreetmap.org/$zoomLvl/$col/$row.png"
            }
        return try {
            getStream(httpClient, baseUrl)
        } catch (e: Exception) {
            Logger.e("Failed to load tile", e)
            null
        }
    }
}
