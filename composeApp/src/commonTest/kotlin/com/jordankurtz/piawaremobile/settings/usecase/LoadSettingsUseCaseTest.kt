package com.jordankurtz.piawaremobile.settings.usecase

import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.impl.LoadSettingsUseCaseImpl
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoadSettingsUseCaseTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var loadSettingsUseCase: LoadSettingsUseCase

    @BeforeTest
    fun setUp() {
        settingsRepository = mock()
        loadSettingsUseCase = LoadSettingsUseCaseImpl(settingsRepository)
    }

    @Test
    fun `invoke returns loading then settings from repository`() = runTest {
        // Given
        val settings = Settings(
            servers = emptyList(),
            refreshInterval = 5,
            centerMapOnUserOnStart = false,
            restoreMapStateOnStart = false,
            showReceiverLocations = false,
            showUserLocationOnMap = false
        )
        every { settingsRepository.getSettings() } returns flowOf(settings)

        // When
        val result = loadSettingsUseCase()

        // Then
        val emissions = result.toList()
        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is Async.Loading)
        assertEquals(Async.Success(settings), emissions[1])
    }

    @Test
    fun `invoke returns distinct values`() = runTest {
        // Given
        val settings = Settings(
            servers = emptyList(),
            refreshInterval = 5,
            centerMapOnUserOnStart = false,
            restoreMapStateOnStart = false,
            showReceiverLocations = false,
            showUserLocationOnMap = false
        )
        every { settingsRepository.getSettings() } returns flowOf(settings, settings)

        // When
        val result = loadSettingsUseCase()

        // Then
        val emissions = result.toList()
        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is Async.Loading)
        assertEquals(Async.Success(settings), emissions[1])
    }
}
