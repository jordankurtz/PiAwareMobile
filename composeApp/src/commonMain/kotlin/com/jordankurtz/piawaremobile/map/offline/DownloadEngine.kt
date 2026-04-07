package com.jordankurtz.piawaremobile.map.offline

import kotlinx.coroutines.flow.Flow

interface DownloadEngine {
    fun download(
        region: OfflineRegion,
        config: TileProviderConfig,
    ): Flow<DownloadProgress>
}
