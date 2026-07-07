package com.jordankurtz.squawkscope.model

data class AircraftPosition(
    val latitude: Double,
    val longitude: Double,
    val altitude: String?,
    val timestamp: Double,
)
