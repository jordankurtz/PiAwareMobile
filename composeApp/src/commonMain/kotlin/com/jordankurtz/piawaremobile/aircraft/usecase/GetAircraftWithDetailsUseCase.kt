package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo

interface GetAircraftWithDetailsUseCase {
    suspend operator fun invoke(servers: List<String>, infoHost: String): List<Pair<Aircraft, AircraftInfo?>>
}
