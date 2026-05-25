package com.jordankurtz.piawaremobile.map.offline

import kotlinx.coroutines.flow.Flow
import com.jordankurtz.piawaremobile.map.TileProviderConfig

interface DownloadEngine {
    fun download(
        region: OfflineRegion,
        config: TileProviderConfig,
    ): Flow<DownloadProgress>
}
