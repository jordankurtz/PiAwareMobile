package com.jordankurtz.piawaremobile.settings

data class Settings (
    val servers: List<Server>,
    val refreshInterval: Int,
)