package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.model.AircraftTrail
import kotlinx.coroutines.flow.Flow

interface GetAircraftTrailUseCase {
    operator fun invoke(hex: String): Flow<AircraftTrail?>
}
