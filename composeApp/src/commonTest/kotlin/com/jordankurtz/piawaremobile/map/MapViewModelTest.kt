package com.jordankurtz.piawaremobile.map

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.jordankurtz.piawaremobile.map.usecase.GetSavedMapStateUseCase
import com.jordankurtz.piawaremobile.map.usecase.SaveMapStateUseCase
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftWithServers
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.MapState
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import com.jordankurtz.piawaremobile.testutil.mockAircraft
import com.jordankurtz.piawaremobile.testutil.mockServer
import com.jordankurtz.piawaremobile.testutil.mockSettings
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ovh.plrapps.mapcompose.core.TileStreamProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class MapViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mapProvider: TileStreamProvider
    private lateinit var getSavedMapStateUseCase: GetSavedMapStateUseCase
    private lateinit var saveMapStateUseCase: SaveMapStateUseCase
    private lateinit var loadSettingsUseCase: LoadSettingsUseCase
    private lateinit var viewModel: MapViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mapProvider = TileStreamProvider { _, _, _ -> null }
        getSavedMapStateUseCase = mock()
        saveMapStateUseCase = mock()
        loadSettingsUseCase = mock()

        everySuspend { getSavedMapStateUseCase() } returns MapState(0.5, 0.5, 1.0)
        everySuspend { saveMapStateUseCase(0.5, 0.5, 1.0) } returns Unit
        every { loadSettingsUseCase() } returns flowOf(Async.Success(mockSettings(restoreMapStateOnStart = false)))

        viewModel = MapViewModel(mapProvider, getSavedMapStateUseCase, saveMapStateUseCase, loadSettingsUseCase)
    }

    @AfterTest
    fun tearDown() {
        viewModel.state.shutdown()
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `fitToAircraft with empty list is a no-op`() =
        runTest {
            viewModel.fitToAircraft(emptyList())
            advanceUntilIdle()
        }

    @Test
    fun `fitToAircraft with single aircraft scrolls to projected point`() =
        runTest {
            val aircraft =
                listOf(
                    AircraftWithServers(
                        aircraft = Aircraft(hex = "abc123", lat = 40.0, lon = -74.0),
                    ),
                )
            viewModel.fitToAircraft(aircraft)
            advanceUntilIdle()
        }

    @Test
    fun `fitToAircraft with multiple aircraft computes bounding box`() =
        runTest {
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
            viewModel.fitToAircraft(aircraft)
            advanceUntilIdle()
        }

    @Test
    fun `fitToAircraft with two aircraft at same location handles degenerate bounding box`() =
        runTest {
            val aircraft =
                listOf(
                    AircraftWithServers(
                        aircraft = Aircraft(hex = "abc123", lat = 40.0, lon = -74.0),
                    ),
                    AircraftWithServers(
                        aircraft = Aircraft(hex = "def456", lat = 40.0, lon = -74.0),
                    ),
                )
            viewModel.fitToAircraft(aircraft)
            advanceUntilIdle()
        }

    @Test
    fun followSelectedAircraftSetsFollowedHexFromSelection() =
        runTest {
            viewModel.syncSelection("ABC123")
            viewModel.followSelectedAircraft()

            viewModel.followingAircraft.test {
                assertEquals("ABC123", awaitItem())
            }
        }

    @Test
    fun unfollowAircraftClearsFollowedHex() =
        runTest {
            viewModel.syncSelection("ABC123")
            viewModel.followSelectedAircraft()
            viewModel.unfollowAircraft()

            viewModel.followingAircraft.test {
                assertNull(awaitItem())
            }
        }

    @Test
    fun onAircraftDeselectedClearsFollowedHex() =
        runTest {
            viewModel.syncSelection("ABC123")
            viewModel.followSelectedAircraft()
            viewModel.onAircraftDeselected()

            viewModel.followingAircraft.test {
                assertNull(awaitItem())
            }
            viewModel.selectedAircraft.test {
                assertNull(awaitItem())
            }
        }

    @Test
    fun followedAircraftClearedWhenDisappearsFromFeed() =
        runTest {
            viewModel.syncSelection("ABC123")
            viewModel.followSelectedAircraft()

            assertEquals("ABC123", viewModel.followingAircraft.value)

            val aircraftList =
                listOf(
                    AircraftWithServers(
                        aircraft = mockAircraft(hex = "DEF456"),
                        info = null,
                        servers = setOf(mockServer()),
                    ),
                )
            viewModel.onAircraftUpdated(aircraftList)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.followingAircraft.test {
                assertNull(awaitItem())
            }
        }

    @Test
    fun followedAircraftNotClearedWhenStillInFeed() =
        runTest {
            viewModel.syncSelection("ABC123")
            viewModel.followSelectedAircraft()

            assertEquals("ABC123", viewModel.followingAircraft.value)

            val aircraftList =
                listOf(
                    AircraftWithServers(
                        aircraft = mockAircraft(hex = "ABC123"),
                        info = null,
                        servers = setOf(mockServer()),
                    ),
                )
            viewModel.onAircraftUpdated(aircraftList)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.followingAircraft.test {
                assertEquals("ABC123", awaitItem())
            }
        }

    @Test
    fun followSelectedAircraftWithNoSelectionSetsNull() =
        runTest {
            viewModel.followSelectedAircraft()

            viewModel.followingAircraft.test {
                assertNull(awaitItem())
            }
        }
}
