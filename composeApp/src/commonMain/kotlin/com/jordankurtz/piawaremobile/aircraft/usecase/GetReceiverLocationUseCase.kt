package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.model.Location

interface GetReceiverLocationUseCase {
    suspend operator fun invoke(server: String): Location?
}
