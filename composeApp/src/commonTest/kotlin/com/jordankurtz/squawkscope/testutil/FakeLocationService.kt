package com.jordankurtz.squawkscope.testutil

import com.jordankurtz.squawkscope.location.LocationService
import com.jordankurtz.squawkscope.model.Location

class FakeLocationService : LocationService {
    private val permissionCallbacks = mutableListOf<(Boolean) -> Unit>()
    private var locationCallback: ((Location) -> Unit)? = null

    var requestPermissionsCallCount = 0
    var stopLocationUpdatesCalled = false

    fun grantPermission() = permissionCallbacks.forEach { it(true) }

    fun denyPermission() = permissionCallbacks.forEach { it(false) }

    fun emitLocation(location: Location) = locationCallback?.invoke(location)

    override fun requestPermissions(onResult: (Boolean) -> Unit) {
        requestPermissionsCallCount++
        permissionCallbacks.add(onResult)
    }

    override fun startLocationUpdates(onLocationUpdate: (Location) -> Unit) {
        locationCallback = onLocationUpdate
    }

    override fun stopLocationUpdates() {
        stopLocationUpdatesCalled = true
    }
}
