package com.jordankurtz.piawaremobile.map.usecase

import com.jordankurtz.piawaremobile.map.repo.MapStateRepository
import com.jordankurtz.piawaremobile.map.usecase.impl.SaveMapStateUseCaseImpl
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class SaveMapStateUseCaseTest {
    private lateinit var mapStateRepository: MapStateRepository
    private lateinit var saveMapStateUseCase: SaveMapStateUseCase
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        mapStateRepository = mock()
        saveMapStateUseCase = SaveMapStateUseCaseImpl(mapStateRepository, testDispatcher)
    }

    @Test
    fun `invoke should call saveMapState on repository`() =
        runTest(testDispatcher) {
            // Given
            val latitude = 0.1
            val longitude = 0.2
            val zoom = 5.0
            everySuspend { mapStateRepository.saveMapState(latitude, longitude, zoom) } returns Unit

            // When
            saveMapStateUseCase(latitude, longitude, zoom)

            // Then
            verifySuspend(exactly(1)) { mapStateRepository.saveMapState(latitude, longitude, zoom) }
        }
}
