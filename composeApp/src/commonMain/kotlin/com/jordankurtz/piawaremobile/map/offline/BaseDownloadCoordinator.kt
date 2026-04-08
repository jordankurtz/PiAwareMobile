package com.jordankurtz.piawaremobile.map.offline

import com.jordankurtz.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

abstract class BaseDownloadCoordinator(
    private val engine: DownloadEngine,
    private val store: OfflineTileStore,
    ioDispatcher: CoroutineDispatcher,
) : BackgroundDownloadCoordinator {
    protected val scope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val _progress = MutableStateFlow<DownloadProgress?>(null)
    override val progress: StateFlow<DownloadProgress?> = _progress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    override val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private var downloadJob: Job? = null

    override fun start(
        region: OfflineRegion,
        config: TileProviderConfig,
    ) {
        if (!_isDownloading.compareAndSet(expect = false, update = true)) return
        downloadJob = scope.launch { executeDownload(region, config) }
    }

    override fun cancel() {
        downloadJob?.cancel()
    }

    protected open fun onStartPlatform(regionName: String) {}

    protected open fun onProgressPlatform(
        downloaded: Long,
        total: Long,
    ) {}

    protected open fun onCompletePlatform(regionName: String) {}

    protected open fun onFailedPlatform(regionName: String) {}

    protected open fun onCancelledPlatform() {}

    private suspend fun executeDownload(
        region: OfflineRegion,
        config: TileProviderConfig,
    ) {
        yield() // honour any cancel() that arrived before the coroutine started
        onStartPlatform(region.name)
        var lastDownloaded = region.downloadedTileCount
        var lastTotal = region.tileCount
        try {
            store.updateDownloadStatus(region.id, DownloadStatus.DOWNLOADING, region.downloadedTileCount)
            engine.download(region, config).collect { progress ->
                lastDownloaded = progress.downloaded
                lastTotal = progress.total
                store.updateDownloadStatus(region.id, DownloadStatus.DOWNLOADING, progress.downloaded)
                _progress.value = progress
                onProgressPlatform(progress.downloaded, progress.total)
                yield()
            }
            onCompletePlatform(region.name)
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                store.updateRegionStats(region.id, lastTotal, 0L)
                store.updateDownloadStatus(region.id, DownloadStatus.PARTIAL, lastDownloaded)
                onCancelledPlatform()
            }
            throw e
        } catch (e: Exception) {
            Logger.e("Download failed for region ${region.id}", e)
            onFailedPlatform(region.name)
        } finally {
            _isDownloading.value = false
            _progress.value = null
            downloadJob = null
        }
    }
}
