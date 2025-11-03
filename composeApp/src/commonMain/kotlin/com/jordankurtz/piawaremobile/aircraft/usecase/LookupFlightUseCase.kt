package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Flight

interface LookupFlightUseCase {
    suspend operator fun invoke(ident: String): Async<Flight?>
}
