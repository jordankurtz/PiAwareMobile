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
class MapViewModelOverlayZoomTest {
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

    private fun createViewModel(
        settings: Settings = Settings(),
        initialZoom: Double = 5.0,
    ): Pair<MapViewModel, FakeMapStateController> {
        every { loadSettingsUseCase.invoke() } returns flowOf(Async.Success(settings))
        val controller = FakeMapStateController()
        controller.zoom = initialZoom
        val vm =
            MapViewModel(
                providerConfigFlow = MutableStateFlow(TileProviders.DEFAULT),
                getSavedMapStateUseCase = getSavedMapStateUseCase,
                saveMapStateUseCase = saveMapStateUseCase,
                loadSettingsUseCase = loadSettingsUseCase,
                settingsService = settingsService,
                tileCacheStatsTracker = TileCacheStatsTracker(),
                mapStateController = controller,
            )
        return vm to controller
    }

    // --- limitZoomToOverlay state flow ---

    @Test
    fun `limitZoomToOverlay defaults to false`() =
        runTest(testDispatcher) {
            val (vm, _) = createViewModel()
            advanceUntilIdle()
            assertFalse(vm.limitZoomToOverlay.value)
        }

    @Test
    fun `limitZoomToOverlay reflects settings value true`() =
        runTest(testDispatcher) {
            val (vm, _) = createViewModel(Settings(limitZoomToOverlay = true))
            advanceUntilIdle()
            assertTrue(vm.limitZoomToOverlay.value)
        }

    @Test
    fun `toggleLimitZoomToOverlay calls setLimitZoomToOverlay with toggled value`() =
        runTest(testDispatcher) {
            val (vm, _) = createViewModel(Settings(limitZoomToOverlay = false))
            advanceUntilIdle()
            vm.toggleLimitZoomToOverlay()
            advanceUntilIdle()
            dev.mokkery.verifySuspend { settingsService.setLimitZoomToOverlay(true) }
        }

    // --- effective zoom limits when setting is off ---

    @Test
    fun `zoom limits use global min-max when limitZoomToOverlay is false`() =
        runTest(testDispatcher) {
            val (_, controller) =
                createViewModel(
                    Settings(
                        limitZoomToOverlay = false,
                        minZoomLevel = 2,
                        maxZoomLevel = 14,
                        showFaaIfrHigh = true,
                    ),
                )
            advanceUntilIdle()
            assertEquals(2.0, controller.lastMinZoom)
            assertEquals(14.0, controller.lastMaxZoom)
        }

    // --- effective zoom limits: single overlay ---

    @Test
    fun `zoom limits clamp to IFR High range when only IFR High active`() =
        runTest(testDispatcher) {
            val (_, controller) =
                createViewModel(
                    Settings(
                        limitZoomToOverlay = true,
                        minZoomLevel = 1,
                        maxZoomLevel = 16,
                        showFaaIfrHigh = true,
                    ),
                )
            advanceUntilIdle()
            // IFR_HIGH = 5..9, global = 1..16 → effective = 5..9
            assertEquals(5.0, controller.lastMinZoom)
            assertEquals(9.0, controller.lastMaxZoom)
        }

    @Test
    fun `zoom limits clamp to IFR Low range when only IFR Low active`() =
        runTest(testDispatcher) {
            val (_, controller) =
                createViewModel(
                    Settings(
                        limitZoomToOverlay = true,
                        minZoomLevel = 1,
                        maxZoomLevel = 16,
                        showFaaIfrLow = true,
                    ),
                )
            advanceUntilIdle()
            // IFR_LOW = 7..12
            assertEquals(7.0, controller.lastMinZoom)
            assertEquals(12.0, controller.lastMaxZoom)
        }

    @Test
    fun `zoom limits clamp to sectional range when only sectional active`() =
        runTest(testDispatcher) {
            val (_, controller) =
                createViewModel(
                    Settings(
                        limitZoomToOverlay = true,
                        minZoomLevel = 1,
                        maxZoomLevel = 16,
                        showFaaCharts = true,
                    ),
                )
            advanceUntilIdle()
            // FAA_SECTIONAL = 8..12
            assertEquals(8.0, controller.lastMinZoom)
            assertEquals(12.0, controller.lastMaxZoom)
        }

    @Test
    fun `zoom limits clamp to airspace range when only airspace active`() =
        runTest(testDispatcher) {
            val (_, controller) =
                createViewModel(
                    Settings(
                        limitZoomToOverlay = true,
                        minZoomLevel = 1,
                        maxZoomLevel = 16,
                        showAirspace = true,
                    ),
                )
            advanceUntilIdle()
            // AIRSPACE = 7..14
            assertEquals(7.0, controller.lastMinZoom)
            assertEquals(14.0, controller.lastMaxZoom)
        }

    // --- effective zoom limits: multiple overlays ---

    @Test
    fun `zoom limits intersect IFR High and IFR Low`() =
        runTest(testDispatcher) {
            val (_, controller) =
                createViewModel(
                    Settings(
                        limitZoomToOverlay = true,
                        minZoomLevel = 1,
                        maxZoomLevel = 16,
                        showFaaIfrHigh = true,
                        showFaaIfrLow = true,
                    ),
                )
            advanceUntilIdle()
            // IFR_HIGH=5..9, IFR_LOW=7..12 → intersection = 7..9
            assertEquals(7.0, controller.lastMinZoom)
            assertEquals(9.0, controller.lastMaxZoom)
        }

    // --- no active overlays ---

    @Test
    fun `zoom limits fall back to global when limitZoomToOverlay is true but no overlay active`() =
        runTest(testDispatcher) {
            val (_, controller) =
                createViewModel(
                    Settings(
                        limitZoomToOverlay = true,
                        minZoomLevel = 3,
                        maxZoomLevel = 15,
                    ),
                )
            advanceUntilIdle()
            assertEquals(3.0, controller.lastMinZoom)
            assertEquals(15.0, controller.lastMaxZoom)
        }

    // --- global min/max constrains overlay range ---

    @Test
    fun `effective limits are further bounded by global min and max`() =
        runTest(testDispatcher) {
            val (_, controller) =
                createViewModel(
                    Settings(
                        limitZoomToOverlay = true,
                        minZoomLevel = 7,
                        // global max narrows IFR High's 5..9
                        maxZoomLevel = 8,
                        showFaaIfrHigh = true,
                    ),
                )
            advanceUntilIdle()
            // IFR_HIGH=5..9 ∩ global=7..8 → 7..8
            assertEquals(7.0, controller.lastMinZoom)
            assertEquals(8.0, controller.lastMaxZoom)
        }

    // --- zoom snap ---

    @Test
    fun `zoom is snapped down to effective max when current zoom is above range`() =
        runTest(testDispatcher) {
            val (_, controller) =
                createViewModel(
                    Settings(
                        limitZoomToOverlay = true,
                        // above IFR High max of 9
                        defaultZoomLevel = 13,
                        minZoomLevel = 1,
                        maxZoomLevel = 16,
                        showFaaIfrHigh = true,
                    ),
                )
            advanceUntilIdle()
            assertEquals(9.0, controller.zoom)
        }

    @Test
    fun `zoom is snapped up to effective min when current zoom is below range`() =
        runTest(testDispatcher) {
            val (_, controller) =
                createViewModel(
                    Settings(
                        limitZoomToOverlay = true,
                        // below IFR High min of 5
                        defaultZoomLevel = 3,
                        minZoomLevel = 1,
                        maxZoomLevel = 16,
                        showFaaIfrHigh = true,
                    ),
                )
            advanceUntilIdle()
            assertEquals(5.0, controller.zoom)
        }

    @Test
    fun `zoom is not changed when already within effective range`() =
        runTest(testDispatcher) {
            val (_, controller) =
                createViewModel(
                    Settings(
                        limitZoomToOverlay = true,
                        // within IFR High range 5..9
                        defaultZoomLevel = 7,
                        minZoomLevel = 1,
                        maxZoomLevel = 16,
                        showFaaIfrHigh = true,
                    ),
                )
            advanceUntilIdle()
            assertEquals(7.0, controller.zoom)
        }

    // --- all four overlays simultaneously ---

    @Test
    fun `zoom limits intersect all four overlays`() =
        runTest(testDispatcher) {
            val (_, controller) =
                createViewModel(
                    Settings(
                        limitZoomToOverlay = true,
                        minZoomLevel = 1,
                        maxZoomLevel = 16,
                        showFaaCharts = true,
                        showFaaIfrLow = true,
                        showFaaIfrHigh = true,
                        showAirspace = true,
                    ),
                )
            advanceUntilIdle()
            // FAA_SECTIONAL=8..12, IFR_LOW=7..12, IFR_HIGH=5..9, AIRSPACE=7..14
            // intersection: max(8,7,5,7)=8 to min(12,12,9,14)=9
            assertEquals(8.0, controller.lastMinZoom)
            assertEquals(9.0, controller.lastMaxZoom)
        }

    // --- empty intersection fallback ---

    @Test
    fun `zoom limits fall back to global when global max is below overlay min`() =
        runTest(testDispatcher) {
            val (_, controller) =
                createViewModel(
                    Settings(
                        limitZoomToOverlay = true,
                        minZoomLevel = 1,
                        maxZoomLevel = 4, // below IFR High min of 5
                        showFaaIfrHigh = true,
                    ),
                )
            advanceUntilIdle()
            // Overlay range 5..9 clamped to global max 4 → 5..4 → empty → fall back to global 1..4
            assertEquals(1.0, controller.lastMinZoom)
            assertEquals(4.0, controller.lastMaxZoom)
        }

    // --- restoreMapStateOnStart + overlay zoom limit ---

    @Test
    fun `restored zoom is snapped to overlay range on startup`() =
        runTest(testDispatcher) {
            everySuspend { getSavedMapStateUseCase.invoke() } returns MapState(0.0, 0.0, 13.0)
            val (_, controller) =
                createViewModel(
                    Settings(
                        limitZoomToOverlay = true,
                        restoreMapStateOnStart = true,
                        minZoomLevel = 1,
                        maxZoomLevel = 16,
                        showFaaIfrHigh = true, // range 5..9; saved zoom 13 should snap to 9
                    ),
                )
            advanceUntilIdle()
            assertEquals(9.0, controller.zoom)
        }
}
