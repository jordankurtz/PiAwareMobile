package com.jordankurtz.squawkscope.aircraft.usecase

import com.jordankurtz.squawkscope.model.AircraftTrail
import kotlinx.coroutines.flow.StateFlow

interface GetAllAircraftTrailsUseCase {
    operator fun invoke(): StateFlow<Map<String, AircraftTrail>>
}
