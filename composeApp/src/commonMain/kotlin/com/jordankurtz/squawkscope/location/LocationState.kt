package com.jordankurtz.squawkscope.location

sealed class LocationState {
    object Idle : LocationState()

    object RequestingPermission : LocationState()

    object PermissionGranted : LocationState()

    object PermissionDenied : LocationState()

    object TrackingLocation : LocationState()
}
