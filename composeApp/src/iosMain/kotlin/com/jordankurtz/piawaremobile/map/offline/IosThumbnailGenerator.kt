package com.jordankurtz.piawaremobile.map.offline

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import maplibre.MapLibreThumbnailController
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosThumbnailGenerator(
    @Suppress("UnusedPrivateProperty") private val ioDispatcher: CoroutineDispatcher,
) : ThumbnailGenerator {
    override suspend fun generate(
        bounds: BoundingBox,
        styleUrl: String,
        thumbnailZoom: Int,
        outputPath: String,
    ): Boolean =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                MapLibreThumbnailController.generateSnapshotWithStyleUrl(
                    styleUrl = styleUrl,
                    minLat = bounds.minLat,
                    maxLat = bounds.maxLat,
                    minLon = bounds.minLon,
                    maxLon = bounds.maxLon,
                    zoomLevel = thumbnailZoom.toDouble(),
                    outputPath = outputPath,
                ) { success: Boolean -> cont.resume(success) }
            }
        }
}
