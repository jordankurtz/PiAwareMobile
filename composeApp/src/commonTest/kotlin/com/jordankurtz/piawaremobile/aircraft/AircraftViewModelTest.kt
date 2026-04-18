package com.jordankurtz.piawaremobile.aircraft

import com.jordankurtz.piawaremobile.UrlHandler
import com.jordankurtz.piawaremobile.aircraft.usecase.GetAircraftWithDetailsUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.GetAllAircraftTrailsUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.GetReceiverLocationUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.LoadAircraftTypesUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.LoadHistoryUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.LookupFlightUseCase
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftTrail
import com.jordankurtz.piawaremobile.model.AircraftWithServers
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration

@ExperimentalCoroutinesApi
class AircraftViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var loadAircraftTypesUseCase: LoadAircraftTypesUseCase
    private lateinit var getAircraftWithDetailsUseCase: GetAircraftWithDetailsUseCase
    private lateinit var getReceiverLocationUseCase: GetReceiverLocationUseCase
    private lateinit var loadSettingsUseCase: LoadSettingsUseCase
    private lateinit var lookupFlightUseCase: LookupFlightUseCase
    private lateinit var loadHistoryUseCase: LoadHistoryUseCase
    private lateinit var getAllAircraftTrailsUseCase: GetAllAircraftTrailsUseCase
    private lateinit var urlHandler: UrlHandler
    private val pollTicker = MutableSharedFlow<Unit>()

    private val servers =
        listOf(
            Server(name = "Test", address = "server1.local"),
        )

    private val settings =
        Settings(
            servers = servers,
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

        loadAircraftTypesUseCase = mock()
        getAircraftWithDetailsUseCase = mock()
        getReceiverLocationUseCase = mock()
        loadSettingsUseCase = mock()
        lookupFlightUseCase = mock()
        loadHistoryUseCase = mock()
        getAllAircraftTrailsUseCase = mock()
        urlHandler = mock()

        every { loadSettingsUseCase() } returns flowOf(Async.Success(settings))
        every { getAllAircraftTrailsUseCase() } returns MutableStateFlow(emptyMap<String, AircraftTrail>())
        everySuspend { loadHistoryUseCase(servers) } returns Unit
        everySuspend { loadAircraftTypesUseCase(servers) } returns Unit
        everySuspend { getAircraftWithDetailsUseCase(servers, servers.first()) } returns emptyList()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun testTickerFlow(interval: Duration): Flow<Unit> = pollTicker

    private fun createViewModel(): AircraftViewModel {
        return AircraftViewModel(
            loadAircraftTypesUseCase = loadAircraftTypesUseCase,
            getAircraftWithDetailsUseCase = getAircraftWithDetailsUseCase,
            getReceiverLocationUseCase = getReceiverLocationUseCase,
            loadSettingsUseCase = loadSettingsUseCase,
            lookupFlightUseCase = lookupFlightUseCase,
            loadHistoryUseCase = loadHistoryUseCase,
            getAllAircraftTrailsUseCase = getAllAircraftTrailsUseCase,
            urlHandler = urlHandler,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher,
            tickerFlow = ::testTickerFlow,
        )
    }

    @Test
    fun `onResume skips first call and reloads history on subsequent calls`() =
        runTest {
            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onResume() // First resume skipped (init already loading history)
            viewModel.onResume() // Second resume should reload history
            testDispatcher.scheduler.advanceUntilIdle()

            // Once from init + once from second onResume
            verifySuspend(mode = VerifyMode.exactly(2)) {
                loadHistoryUseCase(servers)
            }
        }

    @Test
    fun `polling fetches aircraft on each tick`() =
        runTest {
            createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            pollTicker.emit(Unit)
            testDispatcher.scheduler.advanceUntilIdle()

            pollTicker.emit(Unit)
            testDispatcher.scheduler.advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(2)) {
                getAircraftWithDetailsUseCase(servers, servers.first())
            }
        }

    @Test
    fun `empty server list does not start polling or load history`() =
        runTest {
            val emptySettings = settings.copy(servers = emptyList())
            every { loadSettingsUseCase() } returns flowOf(Async.Success(emptySettings))

            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(0, viewModel.numberOfPlanes.value)

            verifySuspend(mode = VerifyMode.exactly(0)) {
                loadHistoryUseCase(listOf())
            }
            verifySuspend(mode = VerifyMode.exactly(0)) {
                loadAircraftTypesUseCase(listOf())
            }
        }

    @Test
    fun `onResume with empty server list does not reload history`() =
        runTest {
            val emptySettings = settings.copy(servers = emptyList())
            every { loadSettingsUseCase() } returns flowOf(Async.Success(emptySettings))

            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onResume() // skip first
            viewModel.onResume() // should not crash or call loadHistory
            testDispatcher.scheduler.advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(0)) {
                loadHistoryUseCase(listOf())
            }
        }

    @Test
    fun `polling updates aircraft count`() =
        runTest {
            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(0, viewModel.numberOfPlanes.value)

            pollTicker.emit(Unit)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(0, viewModel.numberOfPlanes.value) // empty list from mock
        }

    @Test
    fun `selectAircraft triggers flight lookup when API is enabled`() =
        runTest {
            val apiSettings =
                settings.copy(
                    enableFlightAwareApi = true,
                    flightAwareApiKey = "test-key",
                )
            every { loadSettingsUseCase() } returns flowOf(Async.Success(apiSettings))

            val aircraftList =
                listOf(
                    AircraftWithServers(aircraft = Aircraft(hex = "abc123", flight = "UAL123")),
                )
            everySuspend { getAircraftWithDetailsUseCase(servers, servers.first()) } returns aircraftList
            everySuspend { lookupFlightUseCase(any()) } returns Async.Error("test")

            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            // Emit a tick to trigger polling and populate the aircraft list
            pollTicker.emit(Unit)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.selectAircraft("abc123")
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("abc123", viewModel.selectedAircraftHex.value)
            verifySuspend(mode = VerifyMode.exactly(1)) {
                lookupFlightUseCase("UAL123")
            }
        }

    @Test
    fun `selectAircraft sets error when aircraft has no flight number`() =
        runTest {
            val aircraftList =
                listOf(
                    AircraftWithServers(aircraft = Aircraft(hex = "ABC123", flight = null)),
                )
            everySuspend {
                getAircraftWithDetailsUseCase(servers, servers.first())
            } returns aircraftList

            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            pollTicker.emit(Unit)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.selectAircraft("ABC123")
            testDispatcher.scheduler.advanceUntilIdle()

            assertIs<Async.Error>(viewModel.flightDetails.value)
        }

    @Test
    fun `selectAircraft shows feedback when API not configured`() =
        runTest {
            val aircraftList =
                listOf(
                    AircraftWithServers(aircraft = Aircraft(hex = "ABC123", flight = "TST101")),
                )
            everySuspend {
                getAircraftWithDetailsUseCase(servers, servers.first())
            } returns aircraftList
            everySuspend { urlHandler.openUrlInternally(any()) } returns Unit

            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            pollTicker.emit(Unit)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.selectAircraft("ABC123")
            testDispatcher.scheduler.advanceUntilIdle()

            val details = viewModel.flightDetails.value
            assertIs<Async.Error>(details)
            assertTrue(details.message.contains("not configured"))
        }

    @Test
    fun `selectAircraft with null resets flight details`() =
        runTest {
            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.selectAircraft(null)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(null, viewModel.selectedAircraftHex.value)
            assertEquals(Async.NotStarted, viewModel.flightDetails.value)
        }

    @Test
    fun `openFlightPage opens URL internally when openUrlsExternally is false`() =
        runTest {
            val aircraftList =
                listOf(
                    AircraftWithServers(aircraft = Aircraft(hex = "ABC123", flight = "UAL456")),
                )
            everySuspend { getAircraftWithDetailsUseCase(servers, servers.first()) } returns aircraftList
            everySuspend { urlHandler.openUrlInternally(any()) } returns Unit

            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            pollTicker.emit(Unit)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.openFlightPage("ABC123")
            testDispatcher.scheduler.advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) {
                urlHandler.openUrlInternally(any())
            }
        }

    @Test
    fun `openFlightPage opens URL externally when openUrlsExternally is true`() =
        runTest {
            val externalSettings = settings.copy(openUrlsExternally = true)
            every { loadSettingsUseCase() } returns flowOf(Async.Success(externalSettings))

            val aircraftList =
                listOf(
                    AircraftWithServers(aircraft = Aircraft(hex = "ABC123", flight = "UAL456")),
                )
            everySuspend { getAircraftWithDetailsUseCase(servers, servers.first()) } returns aircraftList
            everySuspend { urlHandler.openUrlExternally(any()) } returns Unit

            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            pollTicker.emit(Unit)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.openFlightPage("ABC123")
            testDispatcher.scheduler.advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) {
                urlHandler.openUrlExternally(any())
            }
        }

    @Test
    fun `openFlightPage does nothing when aircraft not found`() =
        runTest {
            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.openFlightPage("unknown")
            testDispatcher.scheduler.advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(0)) {
                urlHandler.openUrlInternally(any())
            }
            verifySuspend(mode = VerifyMode.exactly(0)) {
                urlHandler.openUrlExternally(any())
            }
        }

    @Test
    fun `openFlightPage does nothing when aircraft has no flight number`() =
        runTest {
            val aircraftList =
                listOf(
                    AircraftWithServers(aircraft = Aircraft(hex = "ABC123", flight = null)),
                )
            everySuspend { getAircraftWithDetailsUseCase(servers, servers.first()) } returns aircraftList

            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            pollTicker.emit(Unit)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.openFlightPage("ABC123")
            testDispatcher.scheduler.advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(0)) {
                urlHandler.openUrlInternally(any())
            }
            verifySuspend(mode = VerifyMode.exactly(0)) {
                urlHandler.openUrlExternally(any())
            }
        }

    @Test
    fun `openFlightPage does not change flightDetails state`() =
        runTest {
            val aircraftList =
                listOf(
                    AircraftWithServers(aircraft = Aircraft(hex = "ABC123", flight = "UAL456")),
                )
            everySuspend { getAircraftWithDetailsUseCase(servers, servers.first()) } returns aircraftList
            everySuspend { urlHandler.openUrlInternally(any()) } returns Unit

            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            pollTicker.emit(Unit)
            testDispatcher.scheduler.advanceUntilIdle()

            val stateBefore = viewModel.flightDetails.value
            viewModel.openFlightPage("ABC123")
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(stateBefore, viewModel.flightDetails.value)
        }
}
