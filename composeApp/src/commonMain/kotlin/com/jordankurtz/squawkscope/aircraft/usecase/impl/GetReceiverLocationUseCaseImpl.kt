package com.jordankurtz.squawkscope.aircraft.usecase.impl

import com.jordankurtz.squawkscope.aircraft.repo.AircraftRepo
import com.jordankurtz.squawkscope.aircraft.usecase.GetReceiverLocationUseCase
import com.jordankurtz.squawkscope.di.annotations.IODispatcher
import com.jordankurtz.squawkscope.model.Location
import com.jordankurtz.squawkscope.model.ReceiverType
import com.jordankurtz.squawkscope.settings.Server
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
