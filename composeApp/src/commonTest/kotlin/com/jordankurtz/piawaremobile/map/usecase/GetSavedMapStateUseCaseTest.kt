package com.jordankurtz.piawaremobile.map.usecase

import com.jordankurtz.piawaremobile.map.repo.MapStateRepository
import com.jordankurtz.piawaremobile.map.usecase.impl.GetSavedMapStateUseCaseImpl
import com.jordankurtz.piawaremobile.model.MapState
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetSavedMapStateUseCaseTest {

    private lateinit var mapStateRepository: MapStateRepository
    private lateinit var getSavedMapStateUseCase: GetSavedMapStateUseCase
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        mapStateRepository = mock()
        getSavedMapStateUseCase = GetSavedMapStateUseCaseImpl(mapStateRepository, testDispatcher)
    }

    @Test
    fun `invoke should return map state from repository`() = runTest(testDispatcher) {
        // Given
        val expectedMapState = MapState(scrollX = 0.5, scrollY = 0.5, zoom = 4.0)
        everySuspend { mapStateRepository.getSavedMapState() } returns expectedMapState

        // When
        val result = getSavedMapStateUseCase()

        // Then
        assertEquals(expected = expectedMapState, actual = result)
        verifySuspend(exactly(1)) { mapStateRepository.getSavedMapState() }
    }
}
