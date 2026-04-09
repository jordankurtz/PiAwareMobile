package com.jordankurtz.piawaremobile.map.offline

import org.koin.core.annotation.Single

@Single(binds = [NotificationPermissionService::class])
class NoOpNotificationPermissionService : NotificationPermissionService {
    override fun requestIfNeeded() {}
}
