package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.model.ReceiverType

/**
 * Fetches the configured location of a PiAware receiver.
 */
class GetReceiverLocationUseCase(
    private val aircraftRepo: AircraftRepo
) {
    /**
     * Attempts to get the receiver's location, prioritizing the dump1090
     * endpoint before falling back to the dump978 endpoint.
     *
     * @param server The address of the PiAware device.
     * @return A [Location] object if coordinates are found, otherwise null.
     */
    suspend operator fun invoke(server: String): Location? {
        return (aircraftRepo.getReceiverInfo(server, ReceiverType.DUMP_1090)
            ?: aircraftRepo.getReceiverInfo(
                server,
                ReceiverType.DUMP_978
            ))?.let { Location(it.latitude.toDouble(), it.longitude.toDouble()) }
    }
}
