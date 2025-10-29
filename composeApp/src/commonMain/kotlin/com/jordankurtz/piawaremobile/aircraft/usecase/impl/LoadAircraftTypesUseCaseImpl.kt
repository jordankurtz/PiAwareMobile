package com.jordankurtz.piawaremobile.aircraft.usecase.impl

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.aircraft.usecase.LoadAircraftTypesUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [LoadAircraftTypesUseCase::class])
class LoadAircraftTypesUseCaseImpl(
    private val aircraftRepo: AircraftRepo
) : LoadAircraftTypesUseCase {
    override suspend operator fun invoke(servers: List<String>) {
        aircraftRepo.loadAircraftTypes(servers)
    }
}
