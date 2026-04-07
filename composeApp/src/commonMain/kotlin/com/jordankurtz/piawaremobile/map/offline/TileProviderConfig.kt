package com.jordankurtz.piawaremobile.map.offline

data class TileProviderConfig(
    val id: String,
    val urlTemplate: String,
    val maxConcurrentDownloads: Int,
    val requestDelayMs: Long,
    val avgTileSizeBytes: Long,
    val userAgent: String,
)

object TileProviders {
    val OPENSTREETMAP =
        TileProviderConfig(
            id = "openstreetmap",
            urlTemplate = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
            maxConcurrentDownloads = 2,
            requestDelayMs = 1_000L,
            avgTileSizeBytes = 15_000L,
            userAgent = "PiAwareMobile/1.0 (https://github.com/jordankurtz/PiAwareMobile)",
        )
}
