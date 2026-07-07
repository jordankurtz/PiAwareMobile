package com.jordankurtz.squawkscope.aircraft.usecase

import com.jordankurtz.squawkscope.model.AircraftTrail
import kotlinx.coroutines.flow.Flow

interface GetAircraftTrailUseCase {
    operator fun invoke(hex: String): Flow<AircraftTrail?>
}
