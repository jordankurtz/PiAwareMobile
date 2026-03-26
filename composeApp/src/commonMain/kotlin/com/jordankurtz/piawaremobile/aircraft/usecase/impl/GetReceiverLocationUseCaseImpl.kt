package com.jordankurtz.piawaremobile.aircraft.usecase.impl

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.aircraft.usecase.GetReceiverLocationUseCase
import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.model.ReceiverType
import com.jordankurtz.piawaremobile.settings.Server
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory(binds = [GetReceiverLocationUseCase::class])
class GetReceiverLocationUseCaseImpl(
    private val aircraftRepo: AircraftRepo,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : GetReceiverLocationUseCase {
    override suspend operator fun invoke(server: Server): Location? =
        withContext(ioDispatcher) {
            (
                aircraftRepo.getReceiverInfo(
                    server = server,
                    receiverType = ReceiverType.DUMP_1090,
                ) ?: aircraftRepo.getReceiverInfo(
                    server = server,
                    receiverType = ReceiverType.DUMP_978,
                )
            )?.let {
                Location(
                    latitude = it.latitude.toDouble(),
                    longitude = it.longitude.toDouble(),
                )
            }
        }
}
