package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.settings.Server

interface GetReceiverLocationUseCase {
    suspend operator fun invoke(server: Server): Location?
}
