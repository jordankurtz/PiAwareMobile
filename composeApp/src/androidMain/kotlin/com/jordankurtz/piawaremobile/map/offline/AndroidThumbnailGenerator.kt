package com.jordankurtz.piawaremobile.map.offline

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.snapshotter.MapSnapshot
import org.maplibre.android.snapshotter.MapSnapshotter
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

class AndroidThumbnailGenerator(
    private val context: Context,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher,
) : ThumbnailGenerator {
    override suspend fun generate(
        bounds: BoundingBox,
        styleUrl: String,
        thumbnailZoom: Int,
        outputPath: String,
    ): Boolean {
        val latLngBounds =
            LatLngBounds.Builder()
                .include(LatLng(bounds.maxLat, bounds.maxLon))
                .include(LatLng(bounds.minLat, bounds.minLon))
                .build()
        val center = latLngBounds.center
        val cameraPosition =
            CameraPosition.Builder()
                .target(center)
                .zoom(thumbnailZoom.toDouble())
                .build()
        val options =
            MapSnapshotter.Options(OUTPUT_PX, OUTPUT_PX)
                .withStyle(styleUrl)
                .withRegion(latLngBounds)
                .withCameraPosition(cameraPosition)

        val snapshot: MapSnapshot? =
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { cont ->
                    val snapshotter = MapSnapshotter(context, options)
                    cont.invokeOnCancellation { snapshotter.cancel() }
                    snapshotter.start(
                        { snap: MapSnapshot -> cont.resume(snap) },
                        { cont.resume(null) },
                    )
                }
            }
        snapshot ?: return false

        return withContext(ioDispatcher) {
            runCatching {
                File(outputPath).also { it.parentFile?.mkdirs() }.let { outFile ->
                    FileOutputStream(outFile).use { out ->
                        snapshot.bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
                true
            }.getOrElse { false }
        }
    }

    private companion object {
        const val OUTPUT_PX = 256
    }
}
