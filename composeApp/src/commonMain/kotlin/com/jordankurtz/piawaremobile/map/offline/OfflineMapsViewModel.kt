package com.jordankurtz.piawaremobile.map.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
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
    private val downloadScopeHolder: DownloadScopeHolder,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _regions = MutableStateFlow<List<OfflineRegion>>(emptyList())
    val regions: StateFlow<List<OfflineRegion>> = _regions.asStateFlow()

    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            _regions.value = store.getRegions()
        }
    }

    fun deleteRegion(id: Long) {
        if (_isDownloading.value) return
        viewModelScope.launch(ioDispatcher) {
            store.deleteRegion(id)
            _regions.value = store.getRegions()
        }
    }

    fun startDownload(
        name: String,
        bounds: BoundingBox,
        minZoom: Int,
        maxZoom: Int,
    ) {
        if (!_isDownloading.compareAndSet(expect = false, update = true)) return
        downloadScopeHolder.scope.launch {
            try {
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
                val regionId = withContext(ioDispatcher) { store.saveRegion(region) }
                val savedRegion = region.copy(id = regionId)

                engine.download(savedRegion, TileProviders.OPENSTREETMAP).collect { progress ->
                    _downloadProgress.value = progress
                    // StateFlow is conflated — yield lets collectors observe each progress emission
                    // before the next assignment overwrites it
                    yield()
                }

                // Only refresh regions on successful completion
                withContext(ioDispatcher) { _regions.value = store.getRegions() }
            } catch (e: Exception) {
                Logger.e("Download failed for region", e)
            } finally {
                _isDownloading.value = false
                _downloadProgress.value = null
            }
        }
    }
}
