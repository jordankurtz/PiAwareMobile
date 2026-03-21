package com.jordankurtz.piawaremobile.settings.usecase

import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Settings
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
    private lateinit var settingsService: SettingsService
    private lateinit var loadSettingsUseCase: LoadSettingsUseCase

    private val settings =
        Settings(
            servers = emptyList(),
            refreshInterval = 5,
            centerMapOnUserOnStart = false,
            restoreMapStateOnStart = false,
            showReceiverLocations = false,
            showUserLocationOnMap = false,
            openUrlsExternally = false,
            enableFlightAwareApi = false,
            flightAwareApiKey = "",
        )

    @BeforeTest
    fun setUp() {
        settingsService = mock()
        loadSettingsUseCase = LoadSettingsUseCaseImpl(settingsService)
    }

    @Test
    fun `invoke delegates to settings service`() =
        runTest {
            every { settingsService.loadSettings() } returns
                flowOf(Async.Loading, Async.Success(settings))

            val result = loadSettingsUseCase()
            val emissions = result.toList()

            assertEquals(2, emissions.size)
            assertTrue(emissions[0] is Async.Loading)
            assertEquals(Async.Success(settings), emissions[1])
        }
}
