package com.jordankurtz.piawaremobile.map.offline

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Single

@Single(binds = [BackgroundDownloadCoordinator::class])
class IosBackgroundDownloadCoordinator(
    engine: DownloadEngine,
    store: OfflineTileStore,
    @IODispatcher ioDispatcher: CoroutineDispatcher,
) : BaseDownloadCoordinator(engine, store, ioDispatcher) {
    var observer: IosDownloadObserver? = null

    override fun onStartPlatform(regionName: String) {
        observer?.onDownloadStarting(regionName)
    }

    override fun onProgressPlatform(
        downloaded: Long,
        total: Long,
    ) {
        observer?.onProgress(downloaded, total)
    }

    override fun onCompletePlatform(regionName: String) {
        observer?.onComplete(regionName)
    }

    override fun onFailedPlatform(regionName: String) {
        observer?.onFailed(regionName)
    }

    override fun onCancelledPlatform() {
        observer?.onCancelled()
    }
}
