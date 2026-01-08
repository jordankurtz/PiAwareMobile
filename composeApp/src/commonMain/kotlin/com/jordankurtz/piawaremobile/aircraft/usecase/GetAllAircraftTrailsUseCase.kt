package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.model.AircraftTrail
import kotlinx.coroutines.flow.StateFlow

interface GetAllAircraftTrailsUseCase {
    operator fun invoke(): StateFlow<Map<String, AircraftTrail>>
}
