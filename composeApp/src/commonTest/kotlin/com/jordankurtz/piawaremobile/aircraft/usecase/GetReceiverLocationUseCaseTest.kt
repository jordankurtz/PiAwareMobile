package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.model.Receiver
import com.jordankurtz.piawaremobile.model.ReceiverType
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetReceiverLocationUseCaseTest {

    private lateinit var aircraftRepo: AircraftRepo
    private lateinit var useCase: GetReceiverLocationUseCase

    private val mockReceiverInfo = Receiver(
        latitude = 32.7f,
        longitude = -96.8f
    )

    @BeforeTest
    fun setup() {
        aircraftRepo = mock()
        useCase = GetReceiverLocationUseCase(aircraftRepo)
    }

    @Test
    fun `invoke returns location from dump1090 when available`() = runTest {
        val server = "server1"
        everySuspend { aircraftRepo.getReceiverInfo(server, ReceiverType.DUMP_1090) } returns mockReceiverInfo

        val result = useCase(server)

        val expectedLocation = Location(mockReceiverInfo.latitude.toDouble(), mockReceiverInfo.longitude.toDouble())
        assertEquals(expectedLocation, result)
    }

    @Test
    fun `invoke falls back to dump978 when dump1090 is unavailable`() = runTest {
        val server = "server1"
        everySuspend { aircraftRepo.getReceiverInfo(server, ReceiverType.DUMP_1090) } returns null
        everySuspend { aircraftRepo.getReceiverInfo(server, ReceiverType.DUMP_978) } returns mockReceiverInfo

        val result = useCase(server)

        val expectedLocation = Location(mockReceiverInfo.latitude.toDouble(), mockReceiverInfo.longitude.toDouble())
        assertEquals(expectedLocation, result)
    }

    @Test
    fun `invoke returns null when no location is available`() = runTest {
        val server = "server1"
        everySuspend { aircraftRepo.getReceiverInfo(server, ReceiverType.DUMP_1090) } returns null
        everySuspend { aircraftRepo.getReceiverInfo(server, ReceiverType.DUMP_978) } returns null

        val result = useCase(server)

        assertNull(result)
    }

    @Test
    fun `invoke does not query dump978 when dump1090 is available`() = runTest {
        val server = "server1"
        everySuspend { aircraftRepo.getReceiverInfo(server, ReceiverType.DUMP_1090) } returns mockReceiverInfo

        useCase(server)

        verifySuspend(VerifyMode.not) { aircraftRepo.getReceiverInfo(server, ReceiverType.DUMP_978) }
    }
}
