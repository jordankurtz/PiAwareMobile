package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.aircraft.usecase.impl.GetAircraftWithDetailsUseCaseImpl
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo
import com.jordankurtz.piawaremobile.model.AircraftWithServers
import com.jordankurtz.piawaremobile.settings.Server
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAircraftWithDetailsUseCaseTest {
    private lateinit var aircraftRepo: AircraftRepo
    private lateinit var useCase: GetAircraftWithDetailsUseCase
    private val testDispatcher = StandardTestDispatcher()

    private val server1 = Server(name = "Server 1", address = "server1")
    private val server2 = Server(name = "Server 2", address = "server2")
    private val mockAircraft1 = Aircraft(hex = "a8b2c3", flight = "SWA123", lat = 32.7, lon = -96.8)
    private val mockAircraft2 = Aircraft(hex = "a1b2c3", flight = "DAL456", lat = 32.8, lon = -96.9)
    private val mockAircraftInfo1 =
        AircraftInfo(registration = "N12345", icaoType = "A320", typeDescription = "Airbus A320", wtc = "L")
    private val mockAircraftInfo2 =
        AircraftInfo(registration = "N67890", icaoType = "B738", typeDescription = "Boeing 737-800", wtc = "M")

    @BeforeTest
    fun setup() {
        aircraftRepo = mock()
        useCase = GetAircraftWithDetailsUseCaseImpl(aircraftRepo, testDispatcher)
    }

    @Test
    fun `invoke returns aircraft with details and servers`() =
        runTest(testDispatcher) {
            val servers = listOf(server1, server2)
            val infoServer = server1
            val aircraftWithServers =
                mapOf(
                    mockAircraft1 to setOf(server1, server2),
                    mockAircraft2 to setOf(server1),
                )
            everySuspend { aircraftRepo.getAircraftWithServers(servers) } returns aircraftWithServers
            everySuspend { aircraftRepo.findAircraftInfo(infoServer, mockAircraft1.hex) } returns mockAircraftInfo1
            everySuspend { aircraftRepo.findAircraftInfo(infoServer, mockAircraft2.hex) } returns mockAircraftInfo2

            val result = useCase(servers, infoServer)

            assertEquals(2, result.size)
            val result1 = result.find { it.aircraft.hex == mockAircraft1.hex }!!
            assertEquals(mockAircraft1, result1.aircraft)
            assertEquals(mockAircraftInfo1, result1.info)
            assertEquals(setOf(server1, server2), result1.servers)

            val result2 = result.find { it.aircraft.hex == mockAircraft2.hex }!!
            assertEquals(mockAircraft2, result2.aircraft)
            assertEquals(mockAircraftInfo2, result2.info)
            assertEquals(setOf(server1), result2.servers)
        }

    @Test
    fun `invoke handles case where aircraft info is not found`() =
        runTest(testDispatcher) {
            val servers = listOf(server1, server2)
            val infoServer = server1
            val aircraftWithServers = mapOf(mockAircraft1 to setOf(server1))
            everySuspend { aircraftRepo.getAircraftWithServers(servers) } returns aircraftWithServers
            everySuspend { aircraftRepo.findAircraftInfo(infoServer, mockAircraft1.hex) } returns null

            val result = useCase(servers, infoServer)

            assertEquals(1, result.size)
            assertEquals(AircraftWithServers(mockAircraft1, null, setOf(server1)), result[0])
        }

    @Test
    fun `invoke returns empty list when no aircraft are found`() =
        runTest(testDispatcher) {
            val servers = listOf(server1)
            everySuspend { aircraftRepo.getAircraftWithServers(servers) } returns emptyMap()

            val result = useCase(servers, server1)

            assertEquals(0, result.size)
        }
}
