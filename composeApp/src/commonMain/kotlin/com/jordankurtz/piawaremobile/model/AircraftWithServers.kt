package com.jordankurtz.piawaremobile.model

import com.jordankurtz.piawaremobile.settings.Server

data class AircraftWithServers(
    val aircraft: Aircraft,
    val info: AircraftInfo? = null,
    val servers: Set<Server> = emptySet(),
)
