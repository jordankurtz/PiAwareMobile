package com.jordankurtz.piawaremobile.map.offline

import kotlinx.coroutines.CoroutineDispatcher

class DesktopThumbnailGenerator(
    @Suppress("UnusedPrivateMember")
    private val ioDispatcher: CoroutineDispatcher,
) : ThumbnailGenerator {
    override suspend fun generate(
        bounds: BoundingBox,
        styleUrl: String,
        thumbnailZoom: Int,
        outputPath: String,
    ): Boolean = false
}
