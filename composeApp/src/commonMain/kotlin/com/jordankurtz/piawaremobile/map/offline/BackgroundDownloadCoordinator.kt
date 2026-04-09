package com.jordankurtz.piawaremobile.map.offline

import kotlinx.coroutines.flow.StateFlow

interface BackgroundDownloadCoordinator {
    val progress: StateFlow<DownloadProgress?>
    val isDownloading: StateFlow<Boolean>

    fun start(
        region: OfflineRegion,
        config: TileProviderConfig,
    )

    fun cancel()
}
