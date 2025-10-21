package com.jordankurtz.piawaremobile.location

import com.jordankurtz.piawaremobile.model.Location
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject

actual class LocationService {
    private val locationManager = CLLocationManager()
    // The delegate now needs to be a separate instance to manage its own state.
    private val delegate = LocationDelegate()

    init {
        locationManager.delegate = delegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
    }

    actual fun startLocationUpdates(onLocationUpdate: (Location) -> Unit) {
        // Pass the location update callback to the delegate
        delegate.onLocationUpdate = onLocationUpdate
        locationManager.startUpdatingLocation()
    }

    actual fun stopLocationUpdates() {
        locationManager.stopUpdatingLocation()
        // Clean up the callback
        delegate.onLocationUpdate = null
    }

    actual fun requestPermissions(onResult: (Boolean) -> Unit) {
        // Store the callback in the delegate. It will be called when the user responds.
        delegate.onPermissionResult = onResult

        when (CLLocationManager.authorizationStatus()) {
            // If already granted, immediately return true and clean up.
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> {
                onResult(true)
                delegate.onPermissionResult = null
            }
            // If not determined, request permission. The delegate will now handle the result.
            // DO NOT call onResult here.
            kCLAuthorizationStatusNotDetermined -> {
                locationManager.requestWhenInUseAuthorization()
            }
            // If denied or restricted, immediately return false and clean up.
            else -> {
                onResult(false)
                delegate.onPermissionResult = null
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class LocationDelegate : NSObject(), CLLocationManagerDelegateProtocol {

    // Store callbacks for permission results and location updates.
    var onPermissionResult: ((Boolean) -> Unit)? = null
    var onLocationUpdate: ((Location) -> Unit)? = null

    /**
     * This is the crucial delegate method that gets called after the user
     * allows or denies the location permission request.
     */
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        val status = manager.authorizationStatus

        // Only handle the callback if it exists (i.e., a request was made)
        onPermissionResult?.let { callback ->
            when (status) {
                kCLAuthorizationStatusAuthorizedWhenInUse,
                kCLAuthorizationStatusAuthorizedAlways -> {
                    callback(true) // Permission granted
                }
                kCLAuthorizationStatusDenied -> {
                    callback(false) // Permission denied
                }
                // For other statuses (like not determined), do nothing and wait.
                else -> return
            }
            // Clean up the callback after it has been used.
            onPermissionResult = null
        }
    }

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = (didUpdateLocations.lastOrNull() as? CLLocation) ?: return

        location.coordinate.useContents {
            // Invoke the location update callback if it's set
            onLocationUpdate?.invoke(
                Location(
                    latitude = latitude,
                    longitude = longitude,
                )
            )
        }
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        println("Location error: ${didFailWithError.localizedDescription}")
    }
}
