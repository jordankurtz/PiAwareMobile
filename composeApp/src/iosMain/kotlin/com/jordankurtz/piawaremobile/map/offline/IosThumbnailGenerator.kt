package com.jordankurtz.piawaremobile.map.offline

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGSizeMake
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.writeToFile
import platform.MapLibre.MLNCoordinateBounds
import platform.MapLibre.MLNMapSnapshot
import platform.MapLibre.MLNMapSnapshotOptions
import platform.MapLibre.MLNMapSnapshotter
import platform.UIKit.UIImagePNGRepresentation
import kotlin.coroutines.resume

class IosThumbnailGenerator(
    private val ioDispatcher: CoroutineDispatcher,
) : ThumbnailGenerator {
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    override suspend fun generate(
        bounds: BoundingBox,
        styleUrl: String,
        thumbnailZoom: Int,
        outputPath: String,
    ): Boolean {
        val options =
            MLNMapSnapshotOptions(
                styleURL = NSURL(string = styleUrl),
                region = kotlinx.cinterop.cValue {
                    sw = CLLocationCoordinate2DMake(bounds.minLat, bounds.minLon)
                    ne = CLLocationCoordinate2DMake(bounds.maxLat, bounds.maxLon)
                },
                size = CGSizeMake(OUTPUT_PX.toDouble(), OUTPUT_PX.toDouble()),
            )
        options.zoomLevel = thumbnailZoom.toDouble()

        val snapshot =
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { cont ->
                    val snapshotter = MLNMapSnapshotter(options = options)
                    cont.invokeOnCancellation { snapshotter.cancel() }
                    snapshotter.startWithCompletionHandler { snap: MLNMapSnapshot?, _ ->
                        cont.resume(snap)
                    }
                }
            } ?: return false

        val pngData: NSData = UIImagePNGRepresentation(snapshot.image) ?: return false

        return withContext(ioDispatcher) {
            val outDir = outputPath.substringBeforeLast("/")
            NSFileManager.defaultManager.createDirectoryAtPath(
                outDir,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
            pngData.writeToFile(outputPath, atomically = true)
        }
    }

    private companion object {
        const val OUTPUT_PX = 256
    }
}
