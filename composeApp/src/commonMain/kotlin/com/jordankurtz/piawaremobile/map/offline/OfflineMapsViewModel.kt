package com.jordankurtz.piawaremobile.map.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.cache.TileCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import kotlin.time.Clock

@Factory
class OfflineMapsViewModel(
    private val store: OfflineTileStore,
    private val coordinator: BackgroundDownloadCoordinator,
    private val tileCache: TileCache,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _regions = MutableStateFlow<List<OfflineRegion>>(emptyList())
    val regions: StateFlow<List<OfflineRegion>> = _regions.asStateFlow()

    val downloadProgress: StateFlow<DownloadProgress?> = coordinator.progress
    val isDownloading: StateFlow<Boolean> = coordinator.isDownloading

    private val _pendingDeleteRegion = MutableStateFlow<OfflineRegion?>(null)
    val pendingDeleteRegion: StateFlow<OfflineRegion?> = _pendingDeleteRegion.asStateFlow()

    private val _pendingDeleteFreedBytes = MutableStateFlow(0L)
    val pendingDeleteFreedBytes: StateFlow<Long> = _pendingDeleteFreedBytes.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            store.resetStuckDownloads()
            _regions.value = store.getRegions()
        }
        viewModelScope.launch {
            coordinator.progress.collect { progress ->
                if (progress != null) {
                    _regions.value =
                        _regions.value.map { r ->
                            if (r.id == progress.regionId) {
                                r.copy(downloadedTileCount = progress.downloaded, tileCount = progress.total)
                            } else {
                                r
                            }
                        }
                }
            }
        }
        viewModelScope.launch {
            var wasDownloading = false
            coordinator.isDownloading.collect { downloading ->
                if (wasDownloading && !downloading) {
                    withContext(ioDispatcher) {
                        _regions.value = store.getRegions()
                    }
                }
                wasDownloading = downloading
            }
        }
    }

    fun cancelDownload() {
        coordinator.cancel()
    }

    fun requestDeleteRegion(region: OfflineRegion) {
        if (region.status == DownloadStatus.DOWNLOADING) return
        _pendingDeleteRegion.value = region
        viewModelScope.launch(ioDispatcher) {
            _pendingDeleteFreedBytes.value = store.getFreedBytesForRegion(region.id)
        }
    }

    fun cancelDelete() {
        _pendingDeleteRegion.value = null
        _pendingDeleteFreedBytes.value = 0L
    }

    fun confirmDelete() {
        val region = _pendingDeleteRegion.value ?: return
        _pendingDeleteRegion.value = null
        viewModelScope.launch(ioDispatcher) {
            val exclusiveTiles = store.getExclusiveTilesForRegion(region.id)
            for ((zoom, col, row) in exclusiveTiles) {
                tileCache.delete(zoom, col, row, region.providerId)
            }
            store.deleteRegion(region.id)
            _regions.value = store.getRegions()
        }
    }

    fun startDownload(
        name: String,
        bounds: BoundingBox,
        minZoom: Int,
        maxZoom: Int,
    ) {
        if (coordinator.isDownloading.value) return
        viewModelScope.launch(ioDispatcher) {
            val region =
                OfflineRegion(
                    name = name,
                    minZoom = minZoom,
                    maxZoom = maxZoom,
                    minLat = bounds.minLat,
                    maxLat = bounds.maxLat,
                    minLon = bounds.minLon,
                    maxLon = bounds.maxLon,
                    providerId = TileProviders.OPENSTREETMAP.id,
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                )
            val regionId = store.saveRegion(region)
            val savedRegion = region.copy(id = regionId, status = DownloadStatus.DOWNLOADING)
            _regions.value = _regions.value + savedRegion
            coordinator.start(savedRegion, TileProviders.OPENSTREETMAP)
        }
    }

    fun retryDownload(region: OfflineRegion) {
        if (coordinator.isDownloading.value) return
        viewModelScope.launch(ioDispatcher) {
            _regions.value =
                _regions.value.map { r ->
                    if (r.id == region.id) r.copy(status = DownloadStatus.DOWNLOADING) else r
                }
            coordinator.start(region, TileProviders.OPENSTREETMAP)
        }
    }
}
