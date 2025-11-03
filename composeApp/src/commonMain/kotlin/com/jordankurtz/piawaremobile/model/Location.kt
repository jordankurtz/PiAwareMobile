package com.jordankurtz.piawaremobile.model

import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double
)

fun Location.distanceTo(other: Location): Double {
    val earthRadius = 6371 // in kilometers

    val dLat = (other.latitude - this.latitude) * (PI / 180)
    val dLon = (other.longitude - this.longitude) * (PI / 180)

    val originLat = this.latitude * (PI / 180)
    val destinationLat = other.latitude * (PI / 180)

    val a = sin(dLat / 2) * sin(dLat / 2) +
            sin(dLon / 2) * sin(dLon / 2) * cos(originLat) * cos(destinationLat)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadius * c
}

fun Location.bearingTo(other: Location): Double {
    val dLon = (other.longitude - this.longitude) * (PI / 180)

    val originLat = this.latitude * (PI / 180)
    val destinationLat = other.latitude * (PI / 180)

    val y = sin(dLon) * cos(destinationLat)
    val x = cos(originLat) * sin(destinationLat) - sin(originLat) * cos(destinationLat) * cos(dLon)

    var bearing = atan2(y, x) * (180 / PI)
    bearing = (bearing + 360) % 360

    return bearing
}
