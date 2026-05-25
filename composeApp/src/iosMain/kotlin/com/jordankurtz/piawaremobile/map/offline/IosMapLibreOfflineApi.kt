package com.jordankurtz.piawaremobile.map.offline

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.Foundation.NSData
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.MapLibre.MLNCoordinateBounds
import platform.MapLibre.MLNOfflinePack
import platform.MapLibre.MLNOfflinePackErrorNotification
import platform.MapLibre.MLNOfflinePackProgressChangedNotification
import platform.MapLibre.MLNOfflinePackState
import platform.MapLibre.MLNOfflinePackUserInfoKeyError
import platform.MapLibre.MLNOfflineStorage
import platform.MapLibre.MLNTilePyramidOfflineRegion
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
class IosMapLibreOfflineApi : MapLibreOfflineApi {
    private val storage = MLNOfflineStorage.sharedOfflineStorage

    override suspend fun startDownload(
        styleUrl: String,
        bounds: BoundingBox,
        minZoom: Int,
        maxZoom: Int,
    ): Long = suspendCancellableCoroutine { cont ->
        val coordinateBounds: CValue<MLNCoordinateBounds> = cValue {
            sw = CLLocationCoordinate2DMake(bounds.minLat, bounds.minLon)
            ne = CLLocationCoordinate2DMake(bounds.maxLat, bounds.maxLon)
        }
        val region = MLNTilePyramidOfflineRegion(
            styleURL = NSURL(string = styleUrl),
            bounds = coordinateBounds,
            fromZoomLevel = minZoom.toDouble(),
            toZoomLevel = maxZoom.toDouble(),
        )
        storage.addPackForRegion(
            region,
            withContext = NSData(),
        ) { pack, error ->
            when {
                error != null -> cont.resumeWithException(Exception(error.localizedDescription))
                pack != null -> {
                    pack.resume()
                    cont.resume(pack.hash().toLong())
                }
                else -> cont.resumeWithException(Exception("Failed to create offline pack"))
            }
        }
    }

    override fun observeProgress(nativeRegionId: Long): Flow<DownloadProgress> = callbackFlow {
        val center = NSNotificationCenter.defaultCenter

        val progressObserver = center.addObserverForName(
            name = MLNOfflinePackProgressChangedNotification,
            `object` = null,
            queue = null,
        ) { notification ->
            val pack = notification?.`object` as? MLNOfflinePack ?: return@addObserverForName
            if (pack.hash().toLong() != nativeRegionId) return@addObserverForName
            pack.progress.useContents {
                trySend(
                    DownloadProgress(
                        regionId = nativeRegionId,
                        downloaded = countOfResourcesCompleted.toLong(),
                        total = countOfResourcesExpected.toLong(),
                    ),
                )
            }
            if (pack.state == MLNOfflinePackState.MLNOfflinePackStateComplete) {
                channel.close()
            }
        }

        val errorObserver = center.addObserverForName(
            name = MLNOfflinePackErrorNotification,
            `object` = null,
            queue = null,
        ) { notification ->
            val pack = notification?.`object` as? MLNOfflinePack ?: return@addObserverForName
            if (pack.hash().toLong() != nativeRegionId) return@addObserverForName
            val errorInfo = notification.userInfo
            channel.close(
                Exception(
                    errorInfo?.get(MLNOfflinePackUserInfoKeyError)?.toString() ?: "Download error",
                ),
            )
        }

        awaitClose {
            center.removeObserver(progressObserver)
            center.removeObserver(errorObserver)
        }
    }

    override suspend fun deleteRegion(nativeRegionId: Long): Unit =
        suspendCancellableCoroutine { cont ->
            val pack = storage.packs
                ?.filterIsInstance<MLNOfflinePack>()
                ?.find { it.hash().toLong() == nativeRegionId }
            if (pack == null) {
                cont.resume(Unit)
                return@suspendCancellableCoroutine
            }
            storage.removePack(pack) { error ->
                if (error != null) {
                    cont.resumeWithException(Exception(error.localizedDescription))
                } else {
                    cont.resume(Unit)
                }
            }
        }
}
