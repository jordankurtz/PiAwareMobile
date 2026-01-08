package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.aircraft.usecase.impl.GetAllAircraftTrailsUseCaseImpl
import com.jordankurtz.piawaremobile.model.AircraftTrail
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAllAircraftTrailsUseCaseTest {

    private lateinit var aircraftRepo: AircraftRepo

    private fun createUseCase(): GetAllAircraftTrailsUseCase {
        return GetAllAircraftTrailsUseCaseImpl(aircraftRepo)
    }

    @Test
    fun `invoke returns aircraft trails from repo`() {
        aircraftRepo = mock()
        val trails = mapOf("abc123" to AircraftTrail(hex = "abc123", positions = emptyList()))
        val trailsFlow = MutableStateFlow(trails)

        every { aircraftRepo.aircraftTrails } returns trailsFlow

        val useCase = createUseCase()
        val result = useCase()

        assertEquals(trails, result.value)
    }

    @Test
    fun `invoke returns empty map when no trails`() {
        aircraftRepo = mock()
        val trailsFlow = MutableStateFlow<Map<String, AircraftTrail>>(emptyMap())

        every { aircraftRepo.aircraftTrails } returns trailsFlow

        val useCase = createUseCase()
        val result = useCase()

        assertEquals(emptyMap(), result.value)
    }
}
