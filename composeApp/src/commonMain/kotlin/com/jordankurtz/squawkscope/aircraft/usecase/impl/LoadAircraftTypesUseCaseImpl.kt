package com.jordankurtz.squawkscope.aircraft.usecase.impl

import com.jordankurtz.squawkscope.aircraft.repo.AircraftRepo
import com.jordankurtz.squawkscope.aircraft.usecase.LoadAircraftTypesUseCase
import com.jordankurtz.squawkscope.di.annotations.IODispatcher
import com.jordankurtz.squawkscope.settings.Server
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory(binds = [LoadAircraftTypesUseCase::class])
class LoadAircraftTypesUseCaseImpl(
    private val aircraftRepo: AircraftRepo,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : LoadAircraftTypesUseCase {
    override suspend operator fun invoke(servers: List<Server>) =
        withContext(ioDispatcher) {
            aircraftRepo.loadAircraftTypes(servers)
        }
}
