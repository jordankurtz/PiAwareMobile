package com.jordankurtz.piawaremobile.settings

import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.SettingsService
import com.jordankurtz.piawaremobile.settings.usecase.impl.SettingsServiceImpl
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.capture.Capture.Companion.slot
import dev.mokkery.matcher.capture.SlotCapture
import dev.mokkery.matcher.capture.capture
import dev.mokkery.mock
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsServiceOverlayTest {
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var settingsService: SettingsService
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        settingsRepository = mock()
        settingsService = SettingsServiceImpl(settingsRepository, testDispatcher)
        everySuspend { settingsRepository.getSettings() } returns flowOf(Settings())
        everySuspend { settingsRepository.saveSettings(any()) } returns Unit
    }

    @Test
    fun `setShowFaaCharts persists true`() =
        runTest(testDispatcher) {
            val slot = slot<Settings>()
            everySuspend { settingsRepository.saveSettings(capture(slot)) } returns Unit

            settingsService.setShowFaaCharts(true)

            val saved = (slot.value as SlotCapture.Value.Present).value
            assertTrue(saved.showFaaCharts)
        }

    @Test
    fun `setShowFaaCharts persists false`() =
        runTest(testDispatcher) {
            everySuspend { settingsRepository.getSettings() } returns flowOf(Settings(showFaaCharts = true))
            val slot = slot<Settings>()
            everySuspend { settingsRepository.saveSettings(capture(slot)) } returns Unit

            settingsService.setShowFaaCharts(false)

            val saved = (slot.value as SlotCapture.Value.Present).value
            assertFalse(saved.showFaaCharts)
        }

    @Test
    fun `setShowAirspace persists true`() =
        runTest(testDispatcher) {
            val slot = slot<Settings>()
            everySuspend { settingsRepository.saveSettings(capture(slot)) } returns Unit

            settingsService.setShowAirspace(true)

            val saved = (slot.value as SlotCapture.Value.Present).value
            assertTrue(saved.showAirspace)
        }

    @Test
    fun `setShowAirspace persists false`() =
        runTest(testDispatcher) {
            everySuspend { settingsRepository.getSettings() } returns flowOf(Settings(showAirspace = true))
            val slot = slot<Settings>()
            everySuspend { settingsRepository.saveSettings(capture(slot)) } returns Unit

            settingsService.setShowAirspace(false)

            val saved = (slot.value as SlotCapture.Value.Present).value
            assertFalse(saved.showAirspace)
        }

    @Test
    fun `setLimitZoomToOverlay persists true`() =
        runTest(testDispatcher) {
            val slot = slot<Settings>()
            everySuspend { settingsRepository.saveSettings(capture(slot)) } returns Unit

            settingsService.setLimitZoomToOverlay(true)

            val saved = (slot.value as SlotCapture.Value.Present).value
            assertTrue(saved.limitZoomToOverlay)
        }

    @Test
    fun `setLimitZoomToOverlay persists false`() =
        runTest(testDispatcher) {
            everySuspend { settingsRepository.getSettings() } returns
                flowOf(Settings(limitZoomToOverlay = true))
            val slot = slot<Settings>()
            everySuspend { settingsRepository.saveSettings(capture(slot)) } returns Unit

            settingsService.setLimitZoomToOverlay(false)

            val saved = (slot.value as SlotCapture.Value.Present).value
            assertFalse(saved.limitZoomToOverlay)
        }
}
