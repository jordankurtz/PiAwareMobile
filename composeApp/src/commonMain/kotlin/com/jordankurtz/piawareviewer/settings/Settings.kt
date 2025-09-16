package com.jordankurtz.piawareviewer.settings

data class Settings (
    val servers: List<Server>,
    val refreshInterval: Int,
)