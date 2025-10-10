package com.jordankurtz.piawaremobile.location

expect class LocationService {
    fun startLocationUpdates(onLocationUpdate: (Location) -> Unit)
    fun stopLocationUpdates()
    fun requestPermissions(onResult: (Boolean) -> Unit)
}
