package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.aircraft.usecase.impl.GetAircraftTrailUseCaseImpl
import com.jordankurtz.piawaremobile.model.AircraftPosition
import com.jordankurtz.piawaremobile.model.AircraftTrail
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GetAircraftTrailUseCaseTest {

    private lateinit var aircraftRepo: AircraftRepo

    private fun createUseCase(): GetAircraftTrailUseCase {
        return GetAircraftTrailUseCaseImpl(aircraftRepo)
    }

    @Test
    fun `invoke returns flow that emits trail for aircraft`() = runTest {
        aircraftRepo = mock()
        val trail = AircraftTrail(
            hex = "abc123",
            positions = listOf(
                AircraftPosition(latitude = 32.7, longitude = -96.8, altitude = "35000", timestamp = 1234567890.0)
            )
        )
        val trailsFlow = MutableStateFlow(mapOf("abc123" to trail))

        every { aircraftRepo.aircraftTrails } returns trailsFlow

        val useCase = createUseCase()
        val result = useCase("abc123").first()

        assertNotNull(result)
        assertEquals("abc123", result.hex)
        assertEquals(1, result.positions.size)
    }

    @Test
    fun `invoke returns flow that emits null for unknown aircraft`() = runTest {
        aircraftRepo = mock()
        val trailsFlow = MutableStateFlow<Map<String, AircraftTrail>>(emptyMap())

        every { aircraftRepo.aircraftTrails } returns trailsFlow

        val useCase = createUseCase()
        val result = useCase("unknown").first()

        assertNull(result)
    }

    @Test
    fun `invoke emits updated trail when trails change`() = runTest {
        aircraftRepo = mock()
        val trail1 = AircraftTrail(
            hex = "abc123",
            positions = listOf(
                AircraftPosition(latitude = 32.7, longitude = -96.8, altitude = "35000", timestamp = 1234567890.0)
            )
        )
        val trail2 = AircraftTrail(
            hex = "abc123",
            positions = listOf(
                AircraftPosition(latitude = 32.7, longitude = -96.8, altitude = "35000", timestamp = 1234567890.0),
                AircraftPosition(latitude = 32.8, longitude = -96.9, altitude = "36000", timestamp = 1234567900.0)
            )
        )
        val trailsFlow = MutableStateFlow(mapOf("abc123" to trail1))

        every { aircraftRepo.aircraftTrails } returns trailsFlow

        val useCase = createUseCase()
        val flow = useCase("abc123")

        // First emission
        assertEquals(1, flow.first()?.positions?.size)

        // Update trails
        trailsFlow.value = mapOf("abc123" to trail2)

        // Second emission should have updated trail
        assertEquals(2, flow.first()?.positions?.size)
    }
}
