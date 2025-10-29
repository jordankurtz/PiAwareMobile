package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.aircraft.usecase.impl.LoadAircraftTypesUseCaseImpl
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class LoadAircraftTypesUseCaseTest {

    private lateinit var aircraftRepo: AircraftRepo
    private lateinit var useCase: LoadAircraftTypesUseCase

    @BeforeTest
    fun setup() {
        aircraftRepo = mock()
        useCase = LoadAircraftTypesUseCaseImpl(aircraftRepo)
    }

    @Test
    fun `invoke calls repo to load aircraft types`() = runTest {
        val servers = listOf("server1", "server2")
        everySuspend { aircraftRepo.loadAircraftTypes(servers) } returns Unit

        useCase(servers)

        verifySuspend { aircraftRepo.loadAircraftTypes(servers) }
    }
}
