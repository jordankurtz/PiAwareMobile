package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.aircraft.usecase.impl.LoadHistoryUseCaseImpl
import com.jordankurtz.piawaremobile.settings.Server
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LoadHistoryUseCaseTest {
    private lateinit var aircraftRepo: AircraftRepo

    private val server1 = Server(name = "Server 1", address = "server1.local")
    private val server2 = Server(name = "Server 2", address = "server2.local")
    private val server3 = Server(name = "Server 3", address = "server3.local")

    private fun createUseCase(): LoadHistoryUseCase {
        return LoadHistoryUseCaseImpl(aircraftRepo)
    }

    @Test
    fun `invoke fetches history from all servers`() =
        runTest {
            aircraftRepo = mock()

            val servers = listOf(server1, server2)

            everySuspend { aircraftRepo.fetchAndMergeHistory(server1) } returns Unit
            everySuspend { aircraftRepo.fetchAndMergeHistory(server2) } returns Unit

            val useCase = createUseCase()
            useCase(servers)

            verifySuspend { aircraftRepo.fetchAndMergeHistory(server1) }
            verifySuspend { aircraftRepo.fetchAndMergeHistory(server2) }
        }

    @Test
    fun `invoke does not fetch history when no servers`() =
        runTest {
            aircraftRepo = mock()

            val useCase = createUseCase()
            useCase(emptyList())

            // Should not call fetchAndMergeHistory when there are no servers
        }

    @Test
    fun `invoke can be called multiple times`() =
        runTest {
            aircraftRepo = mock()

            val servers = listOf(server1)

            everySuspend { aircraftRepo.fetchAndMergeHistory(server1) } returns Unit

            val useCase = createUseCase()
            useCase(servers)
            useCase(servers)

            verifySuspend(mode = VerifyMode.exactly(2)) { aircraftRepo.fetchAndMergeHistory(server1) }
        }

    @Test
    fun `invoke continues with other servers when one fails`() =
        runTest {
            aircraftRepo = mock()

            val servers = listOf(server1, server2, server3)

            everySuspend { aircraftRepo.fetchAndMergeHistory(server1) } returns Unit
            everySuspend {
                aircraftRepo.fetchAndMergeHistory(server2)
            } throws RuntimeException("Server unavailable")
            everySuspend { aircraftRepo.fetchAndMergeHistory(server3) } returns Unit

            val useCase = createUseCase()
            useCase(servers)

            // All servers should have been attempted despite the failure
            verifySuspend { aircraftRepo.fetchAndMergeHistory(server1) }
            verifySuspend { aircraftRepo.fetchAndMergeHistory(server2) }
            verifySuspend { aircraftRepo.fetchAndMergeHistory(server3) }
        }
}
