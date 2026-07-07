package com.jordankurtz.squawkscope.aircraft.usecase

import com.jordankurtz.squawkscope.settings.Server

interface LoadAircraftTypesUseCase {
    suspend operator fun invoke(servers: List<Server>)
}
