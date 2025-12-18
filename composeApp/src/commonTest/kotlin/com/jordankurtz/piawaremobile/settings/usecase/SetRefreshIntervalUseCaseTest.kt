package com.jordankurtz.piawaremobile.settings.usecase

import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.impl.SetRefreshIntervalUseCaseImpl
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.capture.Capture.Companion.slot
import dev.mokkery.matcher.capture.SlotCapture
import dev.mokkery.matcher.capture.capture
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SetRefreshIntervalUseCaseTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var setRefreshIntervalUseCase: SetRefreshIntervalUseCase
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        settingsRepository = mock()
        setRefreshIntervalUseCase = SetRefreshIntervalUseCaseImpl(settingsRepository, testDispatcher)
    }

    @Test
    fun `invoke should save settings with new refresh interval`() = runTest(testDispatcher) {
        // Given
        val initialSettings = Settings(
            servers = emptyList(),
            refreshInterval = 5,
            centerMapOnUserOnStart = false,
            restoreMapStateOnStart = false,
            showReceiverLocations = false,
            showUserLocationOnMap = false,
            openUrlsExternally = false,
            enableFlightAwareApi = false,
            flightAwareApiKey = ""
        )
        everySuspend { settingsRepository.getSettings() } returns flowOf(initialSettings)
        val settingsSlot = slot<Settings>()
        everySuspend { settingsRepository.saveSettings(capture(settingsSlot)) } returns Unit
        val newRefreshInterval = 10

        // When
        setRefreshIntervalUseCase(newRefreshInterval = newRefreshInterval)

        // Then
        verifySuspend(mode = VerifyMode.exactly(1)) { settingsRepository.saveSettings(any()) }
        val captured = settingsSlot.value as? SlotCapture.Value.Present
        assertEquals(newRefreshInterval, captured?.value?.refreshInterval)
    }
}
