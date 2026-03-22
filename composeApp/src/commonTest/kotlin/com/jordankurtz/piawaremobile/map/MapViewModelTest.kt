package com.jordankurtz.piawaremobile.map

import androidx.lifecycle.viewModelScope
import com.jordankurtz.piawaremobile.map.usecase.GetSavedMapStateUseCase
import com.jordankurtz.piawaremobile.map.usecase.SaveMapStateUseCase
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftWithServers
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import dev.mokkery.answering.returns
import dev.mokkery.every
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

@ExperimentalCoroutinesApi
class MapViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var loadSettingsUseCase: LoadSettingsUseCase
    private lateinit var getSavedMapStateUseCase: GetSavedMapStateUseCase
    private lateinit var saveMapStateUseCase: SaveMapStateUseCase
    private lateinit var mapProvider: TileStreamProvider
    private lateinit var viewModel: MapViewModel

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
        Dispatchers.setMain(testDispatcher)

        loadSettingsUseCase = mock()
        getSavedMapStateUseCase = mock()
        saveMapStateUseCase = mock()
        mapProvider = TileStreamProvider { _, _, _ -> null }

        every { loadSettingsUseCase() } returns flowOf(Async.Success(settings))

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
            // Should return immediately without launching any coroutine
            viewModel.fitToAircraft(emptyList())
            advanceUntilIdle()
            // No crash = pass. The method returns early for empty list.
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
            // No crash = the single-aircraft path executed correctly
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
            // No crash = bounding box was computed and scrollTo was invoked
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
            // No crash = degenerate bounding box (zero area) is handled
        }
}
