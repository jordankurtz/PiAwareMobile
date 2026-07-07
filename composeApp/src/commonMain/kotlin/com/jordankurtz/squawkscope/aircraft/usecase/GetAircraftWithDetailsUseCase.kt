package com.jordankurtz.squawkscope.aircraft.usecase

import com.jordankurtz.squawkscope.model.AircraftWithServers
import com.jordankurtz.squawkscope.settings.Server

interface GetAircraftWithDetailsUseCase {
    suspend operator fun invoke(
        servers: List<Server>,
        infoServer: Server,
    ): List<AircraftWithServers>
}
