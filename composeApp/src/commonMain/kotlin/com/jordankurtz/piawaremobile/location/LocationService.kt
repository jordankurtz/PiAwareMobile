package com.jordankurtz.piawaremobile.location

import com.jordankurtz.piawaremobile.di.modules.ContextWrapper
import com.jordankurtz.piawaremobile.model.Location
import org.koin.core.annotation.Factory

interface LocationService {
    fun startLocationUpdates(onLocationUpdate: (Location) -> Unit)
    fun stopLocationUpdates()
    fun requestPermissions(onResult: (Boolean) -> Unit)
}

@Factory(binds = [LocationService::class])
expect class LocationServiceImpl(contextWrapper: ContextWrapper): LocationService {
    override fun startLocationUpdates(onLocationUpdate: (Location) -> Unit)
    override fun stopLocationUpdates()
    override fun requestPermissions(onResult: (Boolean) -> Unit)
}
