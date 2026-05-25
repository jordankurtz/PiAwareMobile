package com.jordankurtz.piawaremobile.map.offline

import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidMapLibreOfflineApi(private val context: Context) : MapLibreOfflineApi {
    private val offlineManager = OfflineManager(context)

    override suspend fun startDownload(
        styleUrl: String,
        bounds: BoundingBox,
        minZoom: Int,
        maxZoom: Int,
    ): Long = suspendCancellableCoroutine { cont ->
        val definition = OfflineTilePyramidRegionDefinition(
            styleUrl,
            LatLngBounds.Builder()
                .include(LatLng(bounds.maxLat, bounds.maxLon))
                .include(LatLng(bounds.minLat, bounds.minLon))
                .build(),
            minZoom.toDouble(),
            maxZoom.toDouble(),
            context.resources.displayMetrics.density,
        )
        offlineManager.createOfflineRegion(
            definition,
            byteArrayOf(),
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) {
                    offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
                    cont.resume(offlineRegion.id)
                }

                override fun onError(error: String?) {
                    cont.resumeWithException(Exception(error ?: "Failed to create offline region"))
                }
            },
        )
    }

    override fun observeProgress(nativeRegionId: Long): Flow<DownloadProgress> = callbackFlow {
        var region: OfflineRegion? = null
        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<out OfflineRegion>?) {
                region = offlineRegions?.find { it.id == nativeRegionId }
                if (region == null) {
                    channel.close(Exception("Region $nativeRegionId not found"))
                    return
                }
                region?.setObserver(object : OfflineRegion.OfflineRegionObserver {
                    override fun onStatusChanged(status: OfflineRegionStatus) {
                        trySend(
                            DownloadProgress(
                                regionId = nativeRegionId,
                                downloaded = status.completedTileCount,
                                total = status.requiredResourceCount,
                            ),
                        )
                        if (status.isComplete) {
                            channel.close()
                        }
                    }

                    override fun onError(error: OfflineRegionError) {
                        channel.close(Exception(error.message))
                    }

                    override fun mapboxTileCountLimitExceeded(limit: Long) {
                        channel.close(Exception("Tile count limit exceeded: $limit"))
                    }
                })
            }

            override fun onError(error: String?) {
                channel.close(Exception(error ?: "Failed to list offline regions"))
            }
        })
        awaitClose { region?.setObserver(null) }
    }

    override suspend fun deleteRegion(nativeRegionId: Long): Unit =
        suspendCancellableCoroutine { cont ->
            offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<out OfflineRegion>?) {
                    val region = offlineRegions?.find { it.id == nativeRegionId }
                    if (region == null) {
                        cont.resume(Unit)
                        return
                    }
                    region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                        override fun onDelete() {
                            cont.resume(Unit)
                        }

                        override fun onError(error: String?) {
                            cont.resumeWithException(
                                Exception(error ?: "Failed to delete region"),
                            )
                        }
                    })
                }

                override fun onError(error: String?) {
                    cont.resumeWithException(
                        Exception(error ?: "Failed to list offline regions"),
                    )
                }
            })
        }
}
