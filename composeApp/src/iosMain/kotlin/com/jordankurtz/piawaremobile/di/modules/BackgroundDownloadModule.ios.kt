package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.offline.BackgroundDownloadCoordinator
import com.jordankurtz.piawaremobile.map.offline.DownloadEngine
import com.jordankurtz.piawaremobile.map.offline.IosBackgroundDownloadCoordinator
import com.jordankurtz.piawaremobile.map.offline.OfflineTileStore
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class BackgroundDownloadModule {
    @Single
    actual fun provideBackgroundDownloadCoordinator(
        contextWrapper: ContextWrapper,
        engine: DownloadEngine,
        store: OfflineTileStore,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): BackgroundDownloadCoordinator = IosBackgroundDownloadCoordinator(engine, store, ioDispatcher)
}
