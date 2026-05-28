package com.jordankurtz.piawaremobile.map.offline

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import maplibre.MapLibreObservationToken
import maplibre.MapLibreOfflineController
import platform.Foundation.NSError
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

@OptIn(ExperimentalForeignApi::class)
class IosMapLibreOfflineApi : MapLibreOfflineApi {
    private val controller: MapLibreOfflineController get() = MapLibreOfflineController.shared()!!

    override suspend fun startDownload(
        styleUrl: String,
        bounds: BoundingBox,
        minZoom: Int,
        maxZoom: Int,
    ): Long {
        val nativeId = Random.nextLong()
        return suspendCancellableCoroutine { cont ->
            controller.startDownloadWithStyleUrl(
                styleUrl = styleUrl,
                minLat = bounds.minLat,
                maxLat = bounds.maxLat,
                minLon = bounds.minLon,
                maxLon = bounds.maxLon,
                minZoom = minZoom.toLong(),
                maxZoom = maxZoom.toLong(),
                nativeId = nativeId,
            ) { error: NSError? ->
                if (error != null) {
                    cont.resumeWithException(Exception(error.localizedDescription))
                } else {
                    cont.resume(nativeId)
                }
            }
        }
    }

    override fun observeProgress(nativeRegionId: Long): Flow<DownloadProgress> =
        callbackFlow {
            var token: MapLibreObservationToken? = null
            token =
                controller.observeProgressWithNativeId(
                    nativeId = nativeRegionId,
                    onProgress = { downloaded: Long, total: Long ->
                        trySend(
                            DownloadProgress(
                                regionId = nativeRegionId,
                                downloaded = downloaded,
                                total = total,
                            ),
                        )
                    },
                    onComplete = { channel.close() },
                    onError = { message: String? -> channel.close(Exception(message ?: "Download error")) },
                )
            awaitClose { token?.cancel() }
        }

    override suspend fun deleteRegion(nativeRegionId: Long): Unit =
        suspendCancellableCoroutine { cont ->
            controller.removePackWithNativeId(nativeId = nativeRegionId) { error: NSError? ->
                if (error != null) {
                    cont.resumeWithException(Exception(error.localizedDescription))
                } else {
                    cont.resume(Unit)
                }
            }
        }
}
