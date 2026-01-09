package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.model.AircraftWithServers
import com.jordankurtz.piawaremobile.settings.Server

interface GetAircraftWithDetailsUseCase {
    suspend operator fun invoke(
        servers: List<Server>,
        infoHost: String,
    ): List<AircraftWithServers>
}
