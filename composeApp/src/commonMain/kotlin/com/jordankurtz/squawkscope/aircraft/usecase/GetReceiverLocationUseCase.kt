package com.jordankurtz.squawkscope.aircraft.usecase

import com.jordankurtz.squawkscope.model.Location
import com.jordankurtz.squawkscope.settings.Server

interface GetReceiverLocationUseCase {
    suspend operator fun invoke(server: Server): Location?
}
