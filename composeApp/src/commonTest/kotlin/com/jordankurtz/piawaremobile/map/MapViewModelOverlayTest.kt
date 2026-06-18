package com.jordankurtz.piawaremobile.map

import com.jordankurtz.piawaremobile.map.debug.TileCacheStatsTracker
import com.jordankurtz.piawaremobile.map.usecase.GetSavedMapStateUseCase
import com.jordankurtz.piawaremobile.map.usecase.SaveMapStateUseCase
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.MapState
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SettingsService
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelOverlayTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var settingsService: SettingsService
    private lateinit var loadSettingsUseCase: LoadSettingsUseCase
    private lateinit var getSavedMapStateUseCase: GetSavedMapStateUseCase
    private lateinit var saveMapStateUseCase: SaveMapStateUseCase

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsService = mock()
        loadSettingsUseCase = mock()
        getSavedMapStateUseCase = mock()
        saveMapStateUseCase = mock()
        everySuspend { getSavedMapStateUseCase.invoke() } returns MapState(0.0, 0.0, 8.0)
        everySuspend { saveMapStateUseCase.invoke(any(), any(), any()) } returns Unit
        everySuspend { settingsService.setShowFaaCharts(any()) } returns Unit
        everySuspend { settingsService.setShowAirspace(any()) } returns Unit
        everySuspend { settingsService.setShowFaaIfrLow(any()) } returns Unit
        everySuspend { settingsService.setShowFaaIfrHigh(any()) } returns Unit
        everySuspend { settingsService.setLimitZoomToOverlay(any()) } returns Unit
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(settings: Settings = Settings()): MapViewModel {
        every { loadSettingsUseCase.invoke() } returns flowOf(Async.Success(settings))
        return MapViewModel(
            providerConfigFlow = MutableStateFlow(TileProviders.DEFAULT),
            getSavedMapStateUseCase = getSavedMapStateUseCase,
            saveMapStateUseCase = saveMapStateUseCase,
            loadSettingsUseCase = loadSettingsUseCase,
            settingsService = settingsService,
            tileCacheStatsTracker = TileCacheStatsTracker(),
            mapStateController = FakeMapStateController(),
        )
    }

    @Test
    fun `showFaaCharts reflects settings value true`() =
        runTest(testDispatcher) {
            val vm = createViewModel(Settings(showFaaCharts = true))
            advanceUntilIdle()
            assertTrue(vm.showFaaCharts.value)
        }

    @Test
    fun `showAirspace reflects settings value true`() =
        runTest(testDispatcher) {
            val vm = createViewModel(Settings(showAirspace = true))
            advanceUntilIdle()
            assertTrue(vm.showAirspace.value)
        }

    @Test
    fun `openAipApiKey reflects settings apiKeys`() =
        runTest(testDispatcher) {
            val vm = createViewModel(Settings(apiKeys = mapOf("openaip" to "testkey123")))
            advanceUntilIdle()
            assertEquals("testkey123", vm.openAipApiKey.value)
        }

    @Test
    fun `toggleFaaCharts calls setShowFaaCharts with toggled value`() =
        runTest(testDispatcher) {
            val vm = createViewModel(Settings(showFaaCharts = false))
            advanceUntilIdle()
            vm.toggleFaaCharts()
            advanceUntilIdle()
            verifySuspend { settingsService.setShowFaaCharts(true) }
        }

    @Test
    fun `toggleAirspace calls setShowAirspace with toggled value`() =
        runTest(testDispatcher) {
            val vm = createViewModel(Settings(showAirspace = false))
            advanceUntilIdle()
            vm.toggleAirspace()
            advanceUntilIdle()
            verifySuspend { settingsService.setShowAirspace(true) }
        }

    @Test
    fun `showFaaCharts defaults to false`() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()
            assertFalse(vm.showFaaCharts.value)
        }

    @Test
    fun `showAirspace defaults to false`() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()
            assertFalse(vm.showAirspace.value)
        }

    @Test
    fun `showFaaIfrLow reflects settings value true`() =
        runTest(testDispatcher) {
            val vm = createViewModel(Settings(showFaaIfrLow = true))
            advanceUntilIdle()
            assertTrue(vm.showFaaIfrLow.value)
        }

    @Test
    fun `showFaaIfrLow defaults to false`() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()
            assertFalse(vm.showFaaIfrLow.value)
        }

    @Test
    fun `toggleFaaIfrLow calls setShowFaaIfrLow with toggled value`() =
        runTest(testDispatcher) {
            val vm = createViewModel(Settings(showFaaIfrLow = false))
            advanceUntilIdle()
            vm.toggleFaaIfrLow()
            advanceUntilIdle()
            verifySuspend { settingsService.setShowFaaIfrLow(true) }
        }

    @Test
    fun `showFaaIfrHigh reflects settings value true`() =
        runTest(testDispatcher) {
            val vm = createViewModel(Settings(showFaaIfrHigh = true))
            advanceUntilIdle()
            assertTrue(vm.showFaaIfrHigh.value)
        }

    @Test
    fun `showFaaIfrHigh defaults to false`() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()
            assertFalse(vm.showFaaIfrHigh.value)
        }

    @Test
    fun `toggleFaaIfrHigh calls setShowFaaIfrHigh with toggled value`() =
        runTest(testDispatcher) {
            val vm = createViewModel(Settings(showFaaIfrHigh = false))
            advanceUntilIdle()
            vm.toggleFaaIfrHigh()
            advanceUntilIdle()
            verifySuspend { settingsService.setShowFaaIfrHigh(true) }
        }
}
