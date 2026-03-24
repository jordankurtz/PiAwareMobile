package com.jordankurtz.piawaremobile.map

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.jordankurtz.piawaremobile.map.usecase.GetSavedMapStateUseCase
import com.jordankurtz.piawaremobile.map.usecase.SaveMapStateUseCase
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftWithServers
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.model.MapState
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import com.jordankurtz.piawaremobile.testutil.mockAircraft
import com.jordankurtz.piawaremobile.testutil.mockServer
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.core.TileStreamProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class MapViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mapProvider: TileStreamProvider
    private lateinit var getSavedMapStateUseCase: GetSavedMapStateUseCase
    private lateinit var saveMapStateUseCase: SaveMapStateUseCase
    private lateinit var loadSettingsUseCase: LoadSettingsUseCase
    private lateinit var settingsFlow: MutableStateFlow<Async<Settings>>
    private var viewModel: MapViewModel? = null

    private val settings =
        Settings(
            servers = emptyList(),
            refreshInterval = 5,
            centerMapOnUserOnStart = false,
            restoreMapStateOnStart = false,
            showReceiverLocations = false,
            showUserLocationOnMap = true,
            openUrlsExternally = false,
            enableFlightAwareApi = false,
            flightAwareApiKey = "",
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mapProvider = TileStreamProvider { _, _, _ -> null }
        getSavedMapStateUseCase = mock()
        saveMapStateUseCase = mock()
        loadSettingsUseCase = mock()
        settingsFlow = MutableStateFlow(Async.Success(settings))

        every { loadSettingsUseCase.invoke() } returns settingsFlow
        everySuspend { getSavedMapStateUseCase.invoke() } returns MapState(0.5, 0.5, 1.0)
    }

    @AfterTest
    fun tearDown() {
        viewModel?.viewModelScope?.cancel()
        viewModel?.state?.shutdown()
        viewModel = null
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MapViewModel {
        val vm =
            MapViewModel(
                mapProvider = mapProvider,
                getSavedMapStateUseCase = getSavedMapStateUseCase,
                saveMapStateUseCase = saveMapStateUseCase,
                loadSettingsUseCase = loadSettingsUseCase,
            )
        viewModel = vm
        return vm
    }

    @Test
    fun `fitToAircraft with empty list is a no-op`() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()
            vm.fitToAircraft(emptyList())
            advanceUntilIdle()
        }

    @Test
    fun `fitToAircraft with single aircraft scrolls to projected point`() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()
            val aircraft =
                listOf(
                    AircraftWithServers(
                        aircraft = Aircraft(hex = "abc123", lat = 40.0, lon = -74.0),
                    ),
                )
            vm.fitToAircraft(aircraft)
            advanceUntilIdle()
        }

    @Test
    fun `fitToAircraft with multiple aircraft computes bounding box`() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()
            val aircraft =
                listOf(
                    AircraftWithServers(
                        aircraft = Aircraft(hex = "abc123", lat = 40.0, lon = -74.0),
                    ),
                    AircraftWithServers(
                        aircraft = Aircraft(hex = "def456", lat = 34.0, lon = -118.0),
                    ),
                    AircraftWithServers(
                        aircraft = Aircraft(hex = "ghi789", lat = 41.0, lon = -87.0),
                    ),
                )
            vm.fitToAircraft(aircraft)
            advanceUntilIdle()
        }

    @Test
    fun `fitToAircraft with two aircraft at same location handles degenerate bounding box`() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()
            val aircraft =
                listOf(
                    AircraftWithServers(
                        aircraft = Aircraft(hex = "abc123", lat = 40.0, lon = -74.0),
                    ),
                    AircraftWithServers(
                        aircraft = Aircraft(hex = "def456", lat = 40.0, lon = -74.0),
                    ),
                )
            vm.fitToAircraft(aircraft)
            advanceUntilIdle()
        }

    @Test
    fun `followingUserLocation starts as false`() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()
            assertFalse(vm.followingUserLocation.value)
        }

    @Test
    fun `toggleFollowUserLocation flips state from false to true`() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()
            assertFalse(vm.followingUserLocation.value)

            vm.toggleFollowUserLocation()
            assertTrue(vm.followingUserLocation.value)
        }

    @Test
    fun `toggleFollowUserLocation flips state from true to false`() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()

            vm.toggleFollowUserLocation()
            assertTrue(vm.followingUserLocation.value)

            vm.toggleFollowUserLocation()
            assertFalse(vm.followingUserLocation.value)
        }

    @Test
    fun `showUserLocationOnMap reflects settings value when true`() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()

            assertTrue(vm.showUserLocationOnMap.value)
        }

    @Test
    fun `showUserLocationOnMap reflects settings value when false`() =
        runTest(testDispatcher) {
            settingsFlow.value = Async.Success(settings.copy(showUserLocationOnMap = false))

            val vm = createViewModel()
            advanceUntilIdle()

            assertFalse(vm.showUserLocationOnMap.value)
        }

    @Test
    fun `disabling showUserLocationOnMap also disables following`() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()

            vm.toggleFollowUserLocation()
            assertTrue(vm.followingUserLocation.value)

            settingsFlow.value = Async.Success(settings.copy(showUserLocationOnMap = false))
            advanceUntilIdle()

            assertFalse(vm.followingUserLocation.value)
            assertFalse(vm.showUserLocationOnMap.value)
        }

    @Test
    fun `onUserLocationChanged does not recenter when not following`() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()

            assertFalse(vm.followingUserLocation.value)
            vm.onUserLocationChanged(Location(40.0, -100.0))
            advanceUntilIdle()

            assertNull(vm.lastRecenteredLocation.value)
        }

    @Test
    fun `onUserLocationChanged recenters map when following`() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()

            vm.toggleFollowUserLocation()
            assertTrue(vm.followingUserLocation.value)

            val location = Location(40.0, -100.0)
            vm.onUserLocationChanged(location)
            advanceUntilIdle()

            assertEquals(location, vm.lastRecenteredLocation.value)
        }

    @Test
    fun followSelectedAircraftSetsFollowedHexFromSelection() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()
            vm.syncSelection("ABC123")
            vm.followSelectedAircraft()

            vm.followingAircraft.test {
                assertEquals("ABC123", awaitItem())
            }
        }

    @Test
    fun unfollowAircraftClearsFollowedHex() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()
            vm.syncSelection("ABC123")
            vm.followSelectedAircraft()
            vm.unfollowAircraft()

            vm.followingAircraft.test {
                assertNull(awaitItem())
            }
        }

    @Test
    fun onAircraftDeselectedClearsFollowedHex() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()
            vm.syncSelection("ABC123")
            vm.followSelectedAircraft()
            vm.onAircraftDeselected()

            vm.followingAircraft.test {
                assertNull(awaitItem())
            }
            vm.selectedAircraft.test {
                assertNull(awaitItem())
            }
        }

    @Test
    fun followedAircraftClearedWhenDisappearsFromFeed() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()
            vm.syncSelection("ABC123")
            vm.followSelectedAircraft()

            assertEquals("ABC123", vm.followingAircraft.value)

            val aircraftList =
                listOf(
                    AircraftWithServers(
                        aircraft = mockAircraft(hex = "DEF456"),
                        info = null,
                        servers = setOf(mockServer()),
                    ),
                )
            vm.onAircraftUpdated(aircraftList)
            advanceUntilIdle()

            vm.followingAircraft.test {
                assertNull(awaitItem())
            }
        }

    @Test
    fun followedAircraftNotClearedWhenStillInFeed() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()
            vm.syncSelection("ABC123")
            vm.followSelectedAircraft()

            assertEquals("ABC123", vm.followingAircraft.value)

            val aircraftList =
                listOf(
                    AircraftWithServers(
                        aircraft = mockAircraft(hex = "ABC123"),
                        info = null,
                        servers = setOf(mockServer()),
                    ),
                )
            vm.onAircraftUpdated(aircraftList)
            advanceUntilIdle()

            vm.followingAircraft.test {
                assertEquals("ABC123", awaitItem())
            }
        }

    @Test
    fun followSelectedAircraftWithNoSelectionSetsNull() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            advanceUntilIdle()
            vm.followSelectedAircraft()

            vm.followingAircraft.test {
                assertNull(awaitItem())
            }
        }

    @Test
    fun applyDefaultZoomClampsToMaxWhenDefaultExceedsMax() =
        runTest(testDispatcher) {
            val settings =
                Settings(
                    restoreMapStateOnStart = false,
                    defaultZoomLevel = 14,
                    minZoomLevel = 3,
                    maxZoomLevel = 10,
                )
            loadSettingsUseCase = mock()
            every { loadSettingsUseCase() } returns flowOf(Async.Success(settings))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val expectedScale = scaleForZoomLevel(10)
            assertEquals(expectedScale, viewModel.state.scale)
        }

    @Test
    fun applyDefaultZoomClampsToMinWhenDefaultBelowMin() =
        runTest(testDispatcher) {
            val settings =
                Settings(
                    restoreMapStateOnStart = false,
                    defaultZoomLevel = 1,
                    minZoomLevel = 5,
                    maxZoomLevel = 12,
                )
            loadSettingsUseCase = mock()
            every { loadSettingsUseCase() } returns flowOf(Async.Success(settings))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val expectedScale = scaleForZoomLevel(5)
            assertEquals(expectedScale, viewModel.state.scale)
        }

    @Test
    fun applyDefaultZoomUsesDefaultWhenWithinRange() =
        runTest(testDispatcher) {
            val settings =
                Settings(
                    restoreMapStateOnStart = false,
                    defaultZoomLevel = 8,
                    minZoomLevel = 3,
                    maxZoomLevel = 14,
                )
            loadSettingsUseCase = mock()
            every { loadSettingsUseCase() } returns flowOf(Async.Success(settings))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val expectedScale = scaleForZoomLevel(8)
            assertEquals(expectedScale, viewModel.state.scale)
        }

    @Test
    fun applyDefaultZoomHandlesSwappedMinMax() =
        runTest(testDispatcher) {
            // If min > max in zoom level terms, the effective scale range should still work
            val settings =
                Settings(
                    restoreMapStateOnStart = false,
                    defaultZoomLevel = 14,
                    minZoomLevel = 10,
                    maxZoomLevel = 3,
                )
            loadSettingsUseCase = mock()
            every { loadSettingsUseCase() } returns flowOf(Async.Success(settings))

            val viewModel = createViewModel()
            advanceUntilIdle()

            // Default 14 exceeds the effective max (zoom level 10), so should clamp
            val minScale = scaleForZoomLevel(10)
            val maxScale = scaleForZoomLevel(3)
            val effectiveMin = minOf(minScale, maxScale)
            val effectiveMax = maxOf(minScale, maxScale)
            assertTrue(viewModel.state.scale <= effectiveMax)
            assertTrue(viewModel.state.scale >= effectiveMin)
        }
}
