package com.jordankurtz.piawaremobile.map.offline

import com.jordankurtz.piawaremobile.map.TileProviderConfig
import kotlinx.coroutines.flow.Flow

interface DownloadEngine {
    fun download(
        region: OfflineRegion,
        config: TileProviderConfig,
    ): Flow<DownloadProgress>
}
