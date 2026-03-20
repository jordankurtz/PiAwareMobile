package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.settings.Server

interface LoadAircraftTypesUseCase {
    suspend operator fun invoke(servers: List<Server>)
}
