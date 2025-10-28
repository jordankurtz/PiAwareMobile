package com.jordankurtz.piawaremobile.settings.usecase

import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.impl.AddServerUseCaseImpl
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
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AddServerUseCaseTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var addServerUseCase: AddServerUseCase

    @BeforeTest
    fun setUp() {
        settingsRepository = mock()
        addServerUseCase = AddServerUseCaseImpl(settingsRepository)
    }

    @Test
    fun `invoke should add server to settings`() = runTest {
        // Given
        val initialSettings = Settings(
            servers = emptyList(),
            refreshInterval = 5,
            centerMapOnUserOnStart = false,
            restoreMapStateOnStart = false,
            showReceiverLocations = false,
            showUserLocationOnMap = false
        )
        everySuspend { settingsRepository.getSettings() } returns flowOf(initialSettings)

        val settingsSlot = slot<Settings>()
        everySuspend { settingsRepository.saveSettings(capture(settingsSlot)) } returns Unit

        val serverName = "Test Server"
        val serverAddress = "http://192.168.1.100"

        // When
        addServerUseCase(name = serverName, address = serverAddress)

        // Then
        verifySuspend(VerifyMode.exactly(1)) { settingsRepository.getSettings() }
        verifySuspend(VerifyMode.exactly(1)) { settingsRepository.saveSettings(any()) }

        val capturedSettings = settingsSlot.value as? SlotCapture.Value.Present
        assertEquals(1, capturedSettings?.value?.servers?.size)
        val server = capturedSettings?.value?.servers?.first()
        assertNotNull(server)
        assertEquals(serverName, server.name)
        assertEquals(serverAddress, server.address)
    }
}
