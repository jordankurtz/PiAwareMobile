package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.map.offline.BoundingBox
import com.jordankurtz.piawaremobile.model.Async

interface PrefetchFlightsUseCase {
    suspend operator fun invoke(
        name: String,
        box: BoundingBox,
        daysAhead: Int,
    ): Async<Int>
}
