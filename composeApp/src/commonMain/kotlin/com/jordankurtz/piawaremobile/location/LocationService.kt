package com.jordankurtz.piawaremobile.location

import com.jordankurtz.piawaremobile.model.Location

interface LocationService {
    fun startLocationUpdates(onLocationUpdate: (Location) -> Unit)
    fun stopLocationUpdates()
    fun requestPermissions(onResult: (Boolean) -> Unit)
}

expect class LocationServiceImpl: LocationService {
    override fun startLocationUpdates(onLocationUpdate: (Location) -> Unit)
    override fun stopLocationUpdates()
    override fun requestPermissions(onResult: (Boolean) -> Unit)
}
