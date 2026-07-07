package com.jordankurtz.squawkscope.model

import com.jordankurtz.squawkscope.settings.Server

data class AircraftWithServers(
    val aircraft: Aircraft,
    val info: AircraftInfo? = null,
    val servers: Set<Server> = emptySet(),
)
