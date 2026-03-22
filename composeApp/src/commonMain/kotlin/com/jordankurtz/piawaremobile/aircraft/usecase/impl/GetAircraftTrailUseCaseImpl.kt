package com.jordankurtz.piawaremobile.aircraft.usecase.impl

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftTrailManager
import com.jordankurtz.piawaremobile.aircraft.usecase.GetAircraftTrailUseCase
import com.jordankurtz.piawaremobile.model.AircraftTrail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory

@Factory(binds = [GetAircraftTrailUseCase::class])
class GetAircraftTrailUseCaseImpl(
    private val trailManager: AircraftTrailManager,
) : GetAircraftTrailUseCase {
    override fun invoke(hex: String): Flow<AircraftTrail?> {
        return trailManager.aircraftTrails.map { trails -> trails[hex] }
    }
}
