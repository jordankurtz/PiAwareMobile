package com.jordankurtz.piawaremobile.aircraft.usecase.impl

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftTrailManager
import com.jordankurtz.piawaremobile.aircraft.usecase.GetAllAircraftTrailsUseCase
import com.jordankurtz.piawaremobile.model.AircraftTrail
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

@Factory(binds = [GetAllAircraftTrailsUseCase::class])
class GetAllAircraftTrailsUseCaseImpl(
    private val trailManager: AircraftTrailManager,
) : GetAllAircraftTrailsUseCase {
    override fun invoke(): StateFlow<Map<String, AircraftTrail>> {
        return trailManager.aircraftTrails
    }
}
