package com.jordankurtz.piawaremobile.location

import app.cash.turbine.test
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import com.jordankurtz.piawaremobile.testutil.FakeLocationService
import com.jordankurtz.piawaremobile.testutil.mockSettings
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class LocationViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var locationService: FakeLocationService
    private lateinit var loadSettingsUseCase: LoadSettingsUseCase

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        locationService = FakeLocationService()
        loadSettingsUseCase = mock()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): LocationViewModel =
        LocationViewModel(
            locationService = locationService,
            loadSettingsUseCase = loadSettingsUseCase,
        )

    @Test
    fun `showUserLocationOnMap true requests location permission`() =
        runTest {
            val settings = mockSettings(showUserLocationOnMap = true, centerMapOnUserOnStart = false)
            every { loadSettingsUseCase() } returns flowOf(Async.Success(settings))

            createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, locationService.requestPermissionsCallCount)
        }

    @Test
    fun `centerMapOnUserOnStart true requests location permission`() =
        runTest {
            val settings = mockSettings(showUserLocationOnMap = false, centerMapOnUserOnStart = true)
            every { loadSettingsUseCase() } returns flowOf(Async.Success(settings))

            createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, locationService.requestPermissionsCallCount)
        }

    @Test
    fun `both showUserLocationOnMap and centerMapOnUserOnStart true requests permission twice`() =
        runTest {
            val settings = mockSettings(showUserLocationOnMap = true, centerMapOnUserOnStart = true)
            every { loadSettingsUseCase() } returns flowOf(Async.Success(settings))

            createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(2, locationService.requestPermissionsCallCount)
        }

    @Test
    fun `permission granted transitions state to TrackingLocation`() =
        runTest {
            val settings = mockSettings(showUserLocationOnMap = true, centerMapOnUserOnStart = false)
            every { loadSettingsUseCase() } returns flowOf(Async.Success(settings))

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            locationService.grantPermission()

            assertEquals(LocationState.TrackingLocation, vm.locationState.value)
        }

    @Test
    fun `permission denied transitions state to PermissionDenied`() =
        runTest {
            val settings = mockSettings(showUserLocationOnMap = true, centerMapOnUserOnStart = false)
            every { loadSettingsUseCase() } returns flowOf(Async.Success(settings))

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            locationService.denyPermission()

            assertEquals(LocationState.PermissionDenied, vm.locationState.value)
        }

    @Test
    fun `location updates are reflected in currentLocation`() =
        runTest {
            val settings = mockSettings(showUserLocationOnMap = true, centerMapOnUserOnStart = false)
            every { loadSettingsUseCase() } returns flowOf(Async.Success(settings))
            val location = Location(latitude = 47.6, longitude = -122.3)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            locationService.grantPermission()
            locationService.emitLocation(location)

            assertEquals(location, vm.currentLocation.value)
        }

    @Test
    fun `stopLocationUpdates resets state to Idle and clears location`() =
        runTest {
            val settings = mockSettings(showUserLocationOnMap = true, centerMapOnUserOnStart = false)
            every { loadSettingsUseCase() } returns flowOf(Async.Success(settings))
            val location = Location(latitude = 47.6, longitude = -122.3)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            locationService.grantPermission()
            locationService.emitLocation(location)

            vm.stopLocationUpdates()

            assertEquals(LocationState.Idle, vm.locationState.value)
            assertEquals(null, vm.currentLocation.value)
            assertTrue(locationService.stopLocationUpdatesCalled)
        }

    @Test
    fun `recenterMap emits on first location update when centerMapOnUserOnStart is true`() =
        runTest {
            val settings = mockSettings(showUserLocationOnMap = false, centerMapOnUserOnStart = true)
            every { loadSettingsUseCase() } returns flowOf(Async.Success(settings))
            val location = Location(latitude = 47.6, longitude = -122.3)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            locationService.grantPermission()

            vm.recenterMap.test {
                locationService.emitLocation(location)
                testDispatcher.scheduler.advanceUntilIdle()
                assertEquals(location, awaitItem())
            }
        }

    @Test
    fun `recenterMap does not emit on subsequent location updates`() =
        runTest {
            val settings = mockSettings(showUserLocationOnMap = false, centerMapOnUserOnStart = true)
            every { loadSettingsUseCase() } returns flowOf(Async.Success(settings))
            val location1 = Location(latitude = 47.6, longitude = -122.3)
            val location2 = Location(latitude = 48.0, longitude = -123.0)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            locationService.grantPermission()

            vm.recenterMap.test {
                locationService.emitLocation(location1)
                testDispatcher.scheduler.advanceUntilIdle()
                assertEquals(location1, awaitItem())

                locationService.emitLocation(location2)
                testDispatcher.scheduler.advanceUntilIdle()
                expectNoEvents()
            }
        }

    @Test
    fun `recenterMap does not emit when showUserLocationOnMap true but centerMapOnUserOnStart false`() =
        runTest {
            val settings = mockSettings(showUserLocationOnMap = true, centerMapOnUserOnStart = false)
            every { loadSettingsUseCase() } returns flowOf(Async.Success(settings))
            val location = Location(latitude = 47.6, longitude = -122.3)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            locationService.grantPermission()

            vm.recenterMap.test {
                locationService.emitLocation(location)
                testDispatcher.scheduler.advanceUntilIdle()
                expectNoEvents()
            }
        }
}
