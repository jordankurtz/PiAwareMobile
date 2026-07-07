package com.jordankurtz.squawkscope.aircraft.usecase.impl

import com.jordankurtz.squawkscope.aircraft.repo.AircraftTrailManager
import com.jordankurtz.squawkscope.aircraft.usecase.GetAllAircraftTrailsUseCase
import com.jordankurtz.squawkscope.model.AircraftTrail
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
