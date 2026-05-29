package com.jordankurtz.piawaremobile.map.offline

interface ThumbnailGenerator {
    /**
     * Stitches cached tiles covering [bounds] at [thumbnailZoom] and writes a 256×256px PNG
     * to [outputPath]. Returns false if any required tile is absent from disk — the caller
     * should treat false as a signal to show a placeholder and retry later.
     */
    suspend fun generate(
        bounds: BoundingBox,
        providerId: String,
        thumbnailZoom: Int,
        outputPath: String,
    ): Boolean
}
