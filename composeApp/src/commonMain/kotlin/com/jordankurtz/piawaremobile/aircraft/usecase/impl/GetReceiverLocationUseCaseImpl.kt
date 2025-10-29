package com.jordankurtz.piawaremobile.aircraft.usecase.impl

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.aircraft.usecase.GetReceiverLocationUseCase
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.model.ReceiverType
import org.koin.core.annotation.Factory

@Factory(binds = [GetReceiverLocationUseCase::class])
class GetReceiverLocationUseCaseImpl(
    private val aircraftRepo: AircraftRepo
) : GetReceiverLocationUseCase {
    override suspend operator fun invoke(server: String): Location? {
        return (aircraftRepo.getReceiverInfo(server, ReceiverType.DUMP_1090)
            ?: aircraftRepo.getReceiverInfo(
                server,
                ReceiverType.DUMP_978
            ))?.let { Location(it.latitude.toDouble(), it.longitude.toDouble()) }
    }
}
