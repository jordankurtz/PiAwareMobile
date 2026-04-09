package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.offline.BackgroundDownloadCoordinator
import com.jordankurtz.piawaremobile.map.offline.DesktopBackgroundDownloadCoordinator
import com.jordankurtz.piawaremobile.map.offline.DownloadEngine
import com.jordankurtz.piawaremobile.map.offline.NoOpNotificationPermissionService
import com.jordankurtz.piawaremobile.map.offline.NotificationPermissionService
import com.jordankurtz.piawaremobile.map.offline.OfflineTileStore
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class BackgroundDownloadModule {
    @Single(binds = [NotificationPermissionService::class])
    actual fun provideNotificationPermissionService(contextWrapper: ContextWrapper): NotificationPermissionService =
        NoOpNotificationPermissionService()

    @Single
    actual fun provideBackgroundDownloadCoordinator(
        contextWrapper: ContextWrapper,
        notificationPermissionService: NotificationPermissionService,
        engine: DownloadEngine,
        store: OfflineTileStore,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): BackgroundDownloadCoordinator = DesktopBackgroundDownloadCoordinator(engine, store, ioDispatcher)
}
