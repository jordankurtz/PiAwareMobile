package com.jordankurtz.piawaremobile.settings.usecase

import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.impl.SetRestoreMapStateOnStartUseCaseImpl
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

class SetRestoreMapStateOnStartUseCaseTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var setRestoreMapStateOnStartUseCase: SetRestoreMapStateOnStartUseCase
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        settingsRepository = mock()
        setRestoreMapStateOnStartUseCase = SetRestoreMapStateOnStartUseCaseImpl(settingsRepository, testDispatcher)
    }

    @Test
    fun `invoke with true should save settings with restoreMapStateOnStart as true`() = runTest(testDispatcher) {
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

        // When
        setRestoreMapStateOnStartUseCase(enabled = true)

        // Then
        verifySuspend(mode = VerifyMode.exactly(1)) { settingsRepository.saveSettings(any()) }
        val captured = settingsSlot.value as? SlotCapture.Value.Present
        assertEquals(true, captured?.value?.restoreMapStateOnStart)
    }

    @Test
    fun `invoke with false should save settings with restoreMapStateOnStart as false`() = runTest(testDispatcher) {
        // Given
        val initialSettings = Settings(
            servers = emptyList(),
            refreshInterval = 5,
            centerMapOnUserOnStart = false,
            restoreMapStateOnStart = true,
            showReceiverLocations = false,
            showUserLocationOnMap = false,
            openUrlsExternally = false,
            enableFlightAwareApi = false,
            flightAwareApiKey = ""
        )
        everySuspend { settingsRepository.getSettings() } returns flowOf(initialSettings)
        val settingsSlot = slot<Settings>()
        everySuspend { settingsRepository.saveSettings(capture(settingsSlot)) } returns Unit

        // When
        setRestoreMapStateOnStartUseCase(enabled = false)

        // Then
        verifySuspend(mode = VerifyMode.exactly(1)) { settingsRepository.saveSettings(any()) }
        val captured = settingsSlot.value as? SlotCapture.Value.Present
        assertEquals(false, captured?.value?.restoreMapStateOnStart)
    }
}
