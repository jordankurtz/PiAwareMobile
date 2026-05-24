package com.jordankurtz.piawaremobile.map.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.TileProviderConfig as MapTileProviderConfig
import com.jordankurtz.piawaremobile.map.TileProviders as MapTileProviders
import com.jordankurtz.piawaremobile.map.cache.TileCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.koin.core.annotation.Factory
import kotlin.time.Clock

@Factory
class OfflineMapsViewModel(
    private val store: OfflineTileStore,
    private val engine: DownloadEngine,
    private val tileCache: TileCache,
    private val downloadScopeHolder: DownloadScopeHolder,
    private val thumbnailGenerator: ThumbnailGenerator,
    private val thumbnailFileManager: ThumbnailFileManager,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _regions = MutableStateFlow<List<OfflineRegion>>(emptyList())
    val regions: StateFlow<List<OfflineRegion>> = _regions.asStateFlow()

    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private var downloadJob: Job? = null

    init {
        viewModelScope.launch(ioDispatcher) {
            store.resetStuckDownloads()
            _regions.value = store.getRegions()
            regenerateMissingThumbnails(_regions.value)
        }
    }

    private val _pendingDeleteRegion = MutableStateFlow<OfflineRegion?>(null)
    val pendingDeleteRegion: StateFlow<OfflineRegion?> = _pendingDeleteRegion.asStateFlow()

    private val _pendingDeleteFreedBytes = MutableStateFlow(0L)
    val pendingDeleteFreedBytes: StateFlow<Long> = _pendingDeleteFreedBytes.asStateFlow()

    fun cancelDownload() {
        downloadJob?.cancel()
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
            thumbnailFileManager.delete(region.id)
            _regions.value = store.getRegions()
        }
    }

    fun startDownload(
        name: String,
        bounds: BoundingBox,
        minZoom: Int,
        maxZoom: Int,
        viewportZoom: Int,
        provider: MapTileProviderConfig = MapTileProviders.DEFAULT,
    ) {
        if (!_isDownloading.compareAndSet(expect = false, update = true)) return
        val region =
            OfflineRegion(
                name = name,
                minZoom = minZoom,
                maxZoom = maxZoom,
                minLat = bounds.minLat,
                maxLat = bounds.maxLat,
                minLon = bounds.minLon,
                maxLon = bounds.maxLon,
                providerId = provider.id,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                thumbnailZoom = viewportZoom,
            )
        downloadJob =
            downloadScopeHolder.scope.launch {
                val regionId = store.saveRegion(region)
                // Show the region immediately in the list before tiles start downloading
                _regions.value = _regions.value + region.copy(id = regionId, status = DownloadStatus.DOWNLOADING)

                val outputPath = thumbnailFileManager.thumbnailPath(regionId)
                val generated =
                    thumbnailGenerator.generate(
                        bounds = bounds,
                        providerId = provider.id,
                        thumbnailZoom = viewportZoom,
                        outputPath = outputPath,
                    )
                if (generated) {
                    store.updateThumbnail(regionId, viewportZoom, outputPath)
                }

                doDownload(regionId)
            }
    }

    fun retryDownload(region: OfflineRegion) {
        if (!_isDownloading.compareAndSet(expect = false, update = true)) return
        downloadJob =
            downloadScopeHolder.scope.launch {
                doDownload(region.id)
            }
    }

    private suspend fun doDownload(regionId: Long) {
        var lastDownloadedCount = 0L
        var lastTileCount = 0L
        try {
            store.updateDownloadStatus(regionId, DownloadStatus.DOWNLOADING, 0L)
            _regions.value =
                _regions.value.map { r ->
                    if (r.id == regionId) r.copy(status = DownloadStatus.DOWNLOADING) else r
                }
            val region =
                _regions.value.find { it.id == regionId } ?: run {
                    store.updateDownloadStatus(regionId, DownloadStatus.FAILED, 0L)
                    return
                }
            // Preserve previously downloaded count so cancel-after-retry doesn't regress progress
            lastDownloadedCount = region.downloadedTileCount
            engine.download(region, TileProviders.findById(region.providerId)).collect { progress ->
                lastDownloadedCount = progress.downloaded
                lastTileCount = progress.total
                store.updateDownloadStatus(regionId, DownloadStatus.DOWNLOADING, progress.downloaded)
                _downloadProgress.value = progress
                _regions.value =
                    _regions.value.map { r ->
                        if (r.id == regionId) {
                            r.copy(
                                downloadedTileCount = progress.downloaded,
                                tileCount = progress.total,
                            )
                        } else {
                            r
                        }
                    }
                // StateFlow is conflated — yield lets collectors observe each progress emission
                // before the next assignment overwrites it
                yield()
            }
            // Refresh from DB on completion to pick up final stats and COMPLETE status
            _regions.value = store.getRegions()
            regenerateMissingThumbnails(_regions.value)
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                store.updateRegionStats(regionId, lastTileCount, 0L)
                store.updateDownloadStatus(regionId, DownloadStatus.PARTIAL, lastDownloadedCount)
                _regions.value = store.getRegions()
            }
            throw e
        } catch (e: Exception) {
            Logger.e("Download failed for region", e)
            // Refresh from DB so the FAILED status written by the engine is reflected
            withContext(NonCancellable) { _regions.value = store.getRegions() }
        } finally {
            _isDownloading.value = false
            _downloadProgress.value = null
            downloadJob = null
        }
    }

    private suspend fun regenerateMissingThumbnails(regions: List<OfflineRegion>) {
        regions
            .filter { region ->
                val existingPath = region.thumbnailPath
                region.thumbnailZoom != null &&
                    region.status == DownloadStatus.COMPLETE &&
                    (existingPath == null || !thumbnailFileManager.exists(existingPath))
            }
            .forEach { region ->
                val zoom = region.thumbnailZoom ?: return@forEach
                val path = thumbnailFileManager.thumbnailPath(region.id)
                val ok =
                    thumbnailGenerator.generate(
                        bounds = BoundingBox(region.minLat, region.maxLat, region.minLon, region.maxLon),
                        providerId = region.providerId,
                        thumbnailZoom = zoom,
                        outputPath = path,
                    )
                if (ok) {
                    store.updateThumbnail(region.id, zoom, path)
                    _regions.value = store.getRegions()
                }
            }
    }
}
