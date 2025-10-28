package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.aircraft.usecase.impl.GetAircraftWithDetailsUseCaseImpl
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAircraftWithDetailsUseCaseTest {

    private lateinit var aircraftRepo: AircraftRepo
    private lateinit var useCase: GetAircraftWithDetailsUseCase

    private val mockAircraft1 = Aircraft(hex = "a8b2c3", flight = "SWA123", lat = 32.7, lon = -96.8)
    private val mockAircraft2 = Aircraft(hex = "a1b2c3", flight = "DAL456", lat = 32.8, lon = -96.9)
    private val mockAircraftInfo1 = AircraftInfo(registration = "N12345", icaoType = "A320", typeDescription = "Airbus A320", wtc = "L")
    private val mockAircraftInfo2 = AircraftInfo(registration = "N67890", icaoType = "B738", typeDescription = "Boeing 737-800", wtc = "M")

    @BeforeTest
    fun setup() {
        aircraftRepo = mock()
        useCase = GetAircraftWithDetailsUseCaseImpl(aircraftRepo)
    }

    @Test
    fun `invoke returns aircraft with details`() = runTest {
        val servers = listOf("server1", "server2")
        val infoHost = "server1"
        everySuspend { aircraftRepo.getAircraft(servers) } returns listOf(mockAircraft1, mockAircraft2)
        everySuspend { aircraftRepo.findAircraftInfo(infoHost, mockAircraft1.hex) } returns mockAircraftInfo1
        everySuspend { aircraftRepo.findAircraftInfo(infoHost, mockAircraft2.hex) } returns mockAircraftInfo2

        val result = useCase(servers, infoHost)

        assertEquals(2, result.size)
        assertEquals(Pair(mockAircraft1, mockAircraftInfo1), result[0])
        assertEquals(Pair(mockAircraft2, mockAircraftInfo2), result[1])
    }

    @Test
    fun `invoke handles case where aircraft info is not found`() = runTest {
        val servers = listOf("server1", "server2")
        val infoHost = "server1"
        everySuspend { aircraftRepo.getAircraft(servers) } returns listOf(mockAircraft1)
        everySuspend { aircraftRepo.findAircraftInfo(infoHost, mockAircraft1.hex) } returns null

        val result = useCase(servers, infoHost)

        assertEquals(1, result.size)
        assertEquals(Pair(mockAircraft1, null), result[0])
    }

    @Test
    fun `invoke returns empty list when no aircraft are found`() = runTest {
        val servers = listOf("server1")
        everySuspend { aircraftRepo.getAircraft(servers) } returns emptyList()

        val result = useCase(servers,"server1")

        assertEquals(0, result.size)
    }
}
