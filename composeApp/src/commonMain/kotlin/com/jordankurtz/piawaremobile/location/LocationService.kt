package com.jordankurtz.piawaremobile.location

import com.jordankurtz.piawaremobile.model.Location

expect class LocationService {
    fun startLocationUpdates(onLocationUpdate: (Location) -> Unit)
    fun stopLocationUpdates()
    fun requestPermissions(onResult: (Boolean) -> Unit)
}
