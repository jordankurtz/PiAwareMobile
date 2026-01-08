package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.aircraft.usecase.impl.LoadHistoryUseCaseImpl
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LoadHistoryUseCaseTest {

    private lateinit var aircraftRepo: AircraftRepo

    private fun createUseCase(): LoadHistoryUseCase {
        return LoadHistoryUseCaseImpl(aircraftRepo)
    }

    @Test
    fun `invoke fetches history from all servers`() = runTest {
        aircraftRepo = mock()

        val servers = listOf("server1.local", "server2.local")

        everySuspend { aircraftRepo.fetchAndMergeHistory("server1.local") } returns Unit
        everySuspend { aircraftRepo.fetchAndMergeHistory("server2.local") } returns Unit

        val useCase = createUseCase()
        useCase(servers)

        verifySuspend { aircraftRepo.fetchAndMergeHistory("server1.local") }
        verifySuspend { aircraftRepo.fetchAndMergeHistory("server2.local") }
    }

    @Test
    fun `invoke does not fetch history when no servers`() = runTest {
        aircraftRepo = mock()

        val useCase = createUseCase()
        useCase(emptyList())

        // Should not call fetchAndMergeHistory when there are no servers
    }

    @Test
    fun `invoke only executes once`() = runTest {
        aircraftRepo = mock()

        val servers = listOf("server1.local")

        everySuspend { aircraftRepo.fetchAndMergeHistory("server1.local") } returns Unit

        val useCase = createUseCase()
        useCase(servers)
        useCase(servers) // Second call should be ignored

        verifySuspend { aircraftRepo.fetchAndMergeHistory("server1.local") }
    }

    @Test
    fun `invoke continues with other servers when one fails`() = runTest {
        aircraftRepo = mock()

        val servers = listOf("server1.local", "server2.local", "server3.local")

        everySuspend { aircraftRepo.fetchAndMergeHistory("server1.local") } returns Unit
        everySuspend { aircraftRepo.fetchAndMergeHistory("server2.local") } throws RuntimeException("Server unavailable")
        everySuspend { aircraftRepo.fetchAndMergeHistory("server3.local") } returns Unit

        val useCase = createUseCase()
        useCase(servers)

        // All servers should have been attempted despite the failure
        verifySuspend { aircraftRepo.fetchAndMergeHistory("server1.local") }
        verifySuspend { aircraftRepo.fetchAndMergeHistory("server2.local") }
        verifySuspend { aircraftRepo.fetchAndMergeHistory("server3.local") }
    }
}
