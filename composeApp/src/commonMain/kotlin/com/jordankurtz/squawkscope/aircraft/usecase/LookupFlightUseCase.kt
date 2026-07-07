package com.jordankurtz.squawkscope.aircraft.usecase

import com.jordankurtz.squawkscope.model.Async
import com.jordankurtz.squawkscope.model.Flight

interface LookupFlightUseCase {
    suspend operator fun invoke(ident: String): Async<Flight>
}
