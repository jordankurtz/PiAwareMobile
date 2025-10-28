package com.jordankurtz.piawaremobile.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.jordankurtz.piawaremobile.model.Location

actual class LocationServiceImpl(private val context: Context) : LocationService {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null

    // This property will hold the callback that the ViewModel provides.
    private var onPermissionResultCallback: ((Boolean) -> Unit)? = null

    // This lambda will be set by MainActivity to trigger the permission dialog.
    var permissionLauncher: (() -> Unit)? = null

    actual override fun startLocationUpdates(onLocationUpdate: (Location) -> Unit) {
        val checkSelfPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (checkSelfPermission != PackageManager.PERMISSION_GRANTED
        ) {
            // If permissions are not granted, do not start updates.
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // 5 seconds
        ).apply {
            setMinUpdateIntervalMillis(2000L)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { androidLocation ->
                    onLocationUpdate(
                        Location(
                            latitude = androidLocation.latitude,
                            longitude = androidLocation.longitude
                        )
                    )
                }
            }

            override fun onLocationAvailability(p0: LocationAvailability) {
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    actual override fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    /**
     * Requests location permissions by invoking the launcher provided by MainActivity.
     */
    actual override fun requestPermissions(onResult: (Boolean) -> Unit) {
        // If permission is already granted, invoke the callback immediately and exit.
        val checkSelfPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (checkSelfPermission == PackageManager.PERMISSION_GRANTED
        ) {
            onResult(true)
            return
        }

        // Store the onResult callback from the ViewModel.
        this.onPermissionResultCallback = onResult

        // Trigger the permission dialog via the launcher in MainActivity.
        permissionLauncher?.invoke()
    }

    /**
     * This method is called by MainActivity when the permission result is available.
     * The method name is changed to `onResult` to match the call in MainActivity.
     */
    fun onResult(isGranted: Boolean) {
        // Pass the result back to the ViewModel.
        onPermissionResultCallback?.invoke(isGranted)
        // Clean up the callback to prevent stale references.
        onPermissionResultCallback = null
    }
}
