package com.jordankurtz.piawaremobile.map.offline

data class OfflineRegion(
    val id: String,
    val name: String,
    val minZoom: Int,
    val maxZoom: Int,
    val storageSizeMb: Int,
    val downloadDate: String,
)
