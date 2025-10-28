package com.jordankurtz.piawaremobile.map.usecase

import com.jordankurtz.piawaremobile.map.repo.MapStateRepository
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class SaveMapStateUseCaseTest {

    private lateinit var mapStateRepository: MapStateRepository
    private lateinit var saveMapStateUseCase: SaveMapStateUseCase

    @BeforeTest
    fun setUp() {
        mapStateRepository = mock()
        saveMapStateUseCase = SaveMapStateUseCase(mapStateRepository)
    }

    @Test
    fun `invoke should call saveMapState on repository`() = runTest {
        // Given
        val scrollX = 0.1
        val scrollY = 0.2
        val zoom = 5.0
        everySuspend { mapStateRepository.saveMapState(scrollX, scrollY, zoom) } returns Unit

        // When
        saveMapStateUseCase(scrollX, scrollY, zoom)

        // Then
        verifySuspend(exactly(1)) { mapStateRepository.saveMapState(scrollX, scrollY, zoom) }
    }
}
