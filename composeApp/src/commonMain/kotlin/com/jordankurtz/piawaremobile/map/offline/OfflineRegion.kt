package com.jordankurtz.piawaremobile.map.offline

data class OfflineRegion(
    val id: Long = 0L,
    val name: String,
    val minZoom: Int,
    val maxZoom: Int,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val providerId: String,
    val createdAt: Long,
    val tileCount: Long = 0L,
    val sizeBytes: Long = 0L,
)
