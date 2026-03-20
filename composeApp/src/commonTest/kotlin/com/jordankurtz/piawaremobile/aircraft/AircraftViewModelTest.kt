package com.jordankurtz.piawaremobile.aircraft

import com.jordankurtz.piawaremobile.UrlHandler
import com.jordankurtz.piawaremobile.aircraft.usecase.GetAircraftWithDetailsUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.GetAllAircraftTrailsUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.GetReceiverLocationUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.LoadAircraftTypesUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.LoadHistoryUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.LookupFlightUseCase
import com.jordankurtz.piawaremobile.model.AircraftTrail
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
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
        everySuspend { loadHistoryUseCase(listOf("server1.local")) } returns Unit
        everySuspend { loadAircraftTypesUseCase(listOf("server1.local")) } returns Unit
        everySuspend { getAircraftWithDetailsUseCase(listOf("server1.local"), "server1.local") } returns emptyList()
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
                loadHistoryUseCase(listOf("server1.local"))
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
                getAircraftWithDetailsUseCase(listOf("server1.local"), "server1.local")
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
}
