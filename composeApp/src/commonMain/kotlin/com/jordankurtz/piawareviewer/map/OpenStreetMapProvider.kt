package com.jordankurtz.piawareviewer.map

import io.ktor.client.HttpClient
import kotlinx.io.RawSource
import ovh.plrapps.mapcompose.core.TileStreamProvider

class OpenStreetMapProvider(private val httpClient: HttpClient) : TileStreamProvider {
    override suspend fun getTileStream(row: Int, col: Int, zoomLvl: Int): RawSource? {
        return try {
            getStream(httpClient, "https://tile.openstreetmap.org/$zoomLvl/$col/$row.png")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}