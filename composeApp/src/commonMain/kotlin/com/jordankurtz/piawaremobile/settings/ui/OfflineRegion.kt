package com.jordankurtz.piawaremobile.settings.ui

data class OfflineRegion(
    val id: String,
    val name: String,
    val minZoom: Int,
    val maxZoom: Int,
    val storageSizeMb: Int,
    val downloadDate: String,
)
