package com.jordankurtz.piawaremobile.map

data class LatLon(val latitude: Double, val longitude: Double)

data class MapPosition(val latitude: Double, val longitude: Double, val zoom: Double)

data class MapBounds(
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double,
)
