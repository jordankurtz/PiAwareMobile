package com.jordankurtz.piawaremobile.map.offline

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
class DesktopBackgroundDownloadCoordinator(
    engine: DownloadEngine,
    store: OfflineTileStore,
    @IODispatcher ioDispatcher: CoroutineDispatcher,
) : BaseDownloadCoordinator(engine, store, ioDispatcher)
