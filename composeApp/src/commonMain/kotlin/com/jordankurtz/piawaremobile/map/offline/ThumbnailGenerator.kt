package com.jordankurtz.piawaremobile.map.offline

interface ThumbnailGenerator {
    /**
     * Renders a 256×256px PNG snapshot of [bounds] using [styleUrl] at [thumbnailZoom]
     * and writes it to [outputPath]. Returns false on any failure.
     */
    suspend fun generate(
        bounds: BoundingBox,
        styleUrl: String,
        thumbnailZoom: Int,
        outputPath: String,
    ): Boolean
}
