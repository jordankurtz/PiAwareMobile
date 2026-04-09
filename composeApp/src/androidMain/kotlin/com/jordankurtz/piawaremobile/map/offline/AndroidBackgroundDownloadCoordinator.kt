package com.jordankurtz.piawaremobile.map.offline

import android.content.Intent
import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.di.modules.ContextWrapper
import kotlinx.coroutines.CoroutineDispatcher
class AndroidBackgroundDownloadCoordinator(
    private val contextWrapper: ContextWrapper,
    engine: DownloadEngine,
    store: OfflineTileStore,
    @IODispatcher ioDispatcher: CoroutineDispatcher,
) : BaseDownloadCoordinator(engine, store, ioDispatcher) {
    override fun onStartPlatform(regionName: String) {
        contextWrapper.context.startForegroundService(
            serviceIntent(OfflineDownloadForegroundService.ACTION_START) {
                putExtra(OfflineDownloadForegroundService.EXTRA_REGION_NAME, regionName)
            },
        )
    }

    override fun onCompletePlatform(regionName: String) {
        contextWrapper.context.startService(
            serviceIntent(OfflineDownloadForegroundService.ACTION_COMPLETE) {
                putExtra(OfflineDownloadForegroundService.EXTRA_REGION_NAME, regionName)
            },
        )
    }

    override fun onFailedPlatform(regionName: String) {
        contextWrapper.context.startService(
            serviceIntent(OfflineDownloadForegroundService.ACTION_FAILED) {
                putExtra(OfflineDownloadForegroundService.EXTRA_REGION_NAME, regionName)
            },
        )
    }

    override fun onCancelledPlatform() {
        contextWrapper.context.startService(
            serviceIntent(OfflineDownloadForegroundService.ACTION_CANCELLED),
        )
    }

    private fun serviceIntent(
        action: String,
        block: Intent.() -> Unit = {},
    ): Intent =
        Intent(contextWrapper.context, OfflineDownloadForegroundService::class.java)
            .apply { this.action = action }
            .apply(block)
}
