package com.jordankurtz.piawaremobile.settings

import app.cash.turbine.test
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.usecase.AddServerUseCase
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetCenterMapOnUserOnStartUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetEnableFlightAwareApiUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetFlightAwareApiKeyUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetOpenUrlsExternallyUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetRefreshIntervalUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetRestoreMapStateOnStartUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetShowReceiverLocationsUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetShowUserLocationOnMapUseCase
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
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

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var loadSettingsUseCase: LoadSettingsUseCase
    private lateinit var addServerUseCase: AddServerUseCase
    private lateinit var setRefreshIntervalUseCase: SetRefreshIntervalUseCase
    private lateinit var setCenterMapOnUserOnStartUseCase: SetCenterMapOnUserOnStartUseCase
    private lateinit var setRestoreMapStateOnStartUseCase: SetRestoreMapStateOnStartUseCase
    private lateinit var setShowReceiverLocationsUseCase: SetShowReceiverLocationsUseCase
    private lateinit var setShowUserLocationOnMapUseCase: SetShowUserLocationOnMapUseCase
    private lateinit var setOpenUrlsExternallyUseCase: SetOpenUrlsExternallyUseCase
    private lateinit var setEnableFlightAwareApiUseCase: SetEnableFlightAwareApiUseCase
    private lateinit var setFlightAwareApiKeyUseCase: SetFlightAwareApiKeyUseCase

    private lateinit var viewModel: SettingsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        loadSettingsUseCase = mock()
        addServerUseCase = mock()
        setRefreshIntervalUseCase = mock()
        setCenterMapOnUserOnStartUseCase = mock()
        setRestoreMapStateOnStartUseCase = mock()
        setShowReceiverLocationsUseCase = mock()
        setShowUserLocationOnMapUseCase = mock()
        setOpenUrlsExternallyUseCase = mock()
        setEnableFlightAwareApiUseCase = mock()
        setFlightAwareApiKeyUseCase = mock()

        val settings = Settings(
            servers = emptyList(),
            refreshInterval = 5,
            centerMapOnUserOnStart = false,
            restoreMapStateOnStart = false,
            showReceiverLocations = false,
            showUserLocationOnMap = false,
            openUrlsExternally = false,
            enableFlightAwareApi = false,
            flightAwareApiKey = ""
        )
        every { loadSettingsUseCase() } returns flowOf(Async.Success(settings))

        viewModel = SettingsViewModel(
            loadSettingsUseCase = loadSettingsUseCase,
            addServerUseCase = addServerUseCase,
            setRefreshIntervalUseCase = setRefreshIntervalUseCase,
            setCenterMapOnUserOnStartUseCase = setCenterMapOnUserOnStartUseCase,
            setRestoreMapStateOnStartUseCase = setRestoreMapStateOnStartUseCase,
            setShowReceiverLocationsUseCase = setShowReceiverLocationsUseCase,
            setShowUserLocationOnMapUseCase = setShowUserLocationOnMapUseCase,
            setOpenUrlsExternallyUseCase = setOpenUrlsExternallyUseCase,
            setEnableFlightAwareApiUseCase = setEnableFlightAwareApiUseCase,
            setFlightAwareApiKeyUseCase = setFlightAwareApiKeyUseCase
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `settings flow emits settings from use case`() = runTest {
        val settings = Settings(
            servers = emptyList(),
            refreshInterval = 10,
            centerMapOnUserOnStart = true,
            restoreMapStateOnStart = true,
            showReceiverLocations = true,
            showUserLocationOnMap = true,
            openUrlsExternally = true,
            enableFlightAwareApi = true,
            flightAwareApiKey = "test_key"
        )
        every { loadSettingsUseCase() } returns flowOf(Async.Success(settings))

        val newViewModel = SettingsViewModel(
            loadSettingsUseCase = loadSettingsUseCase,
            addServerUseCase = addServerUseCase,
            setRefreshIntervalUseCase = setRefreshIntervalUseCase,
            setCenterMapOnUserOnStartUseCase = setCenterMapOnUserOnStartUseCase,
            setRestoreMapStateOnStartUseCase = setRestoreMapStateOnStartUseCase,
            setShowReceiverLocationsUseCase = setShowReceiverLocationsUseCase,
            setShowUserLocationOnMapUseCase = setShowUserLocationOnMapUseCase,
            setOpenUrlsExternallyUseCase = setOpenUrlsExternallyUseCase,
            setEnableFlightAwareApiUseCase = setEnableFlightAwareApiUseCase,
            setFlightAwareApiKeyUseCase = setFlightAwareApiKeyUseCase
        )
        newViewModel.settings.test {
            assertEquals(Async.NotStarted, awaitItem())
            assertEquals(Async.Success(settings), awaitItem())
        }
    }

    @Test
    fun `addServer calls addServerUseCase`() = runTest {
        val serverName = "Test Server"
        val serverAddress = "http://test.com"
        everySuspend { addServerUseCase(name = serverName, address = serverAddress) } returns Unit

        viewModel.addServer(name = serverName, address = serverAddress)
        testDispatcher.scheduler.advanceUntilIdle()

        verifySuspend(mode = VerifyMode.exactly(1)) {
            addServerUseCase(
                name = serverName,
                address = serverAddress
            )
        }
    }

    @Test
    fun `updateRefreshInterval calls setRefreshIntervalUseCase`() = runTest {
        val interval = 15
        everySuspend { setRefreshIntervalUseCase(newRefreshInterval = interval) } returns Unit

        viewModel.updateRefreshInterval(refreshInterval = interval)
        testDispatcher.scheduler.advanceUntilIdle()

        verifySuspend(mode = VerifyMode.exactly(1)) { setRefreshIntervalUseCase(newRefreshInterval = interval) }
    }

    @Test
    fun `updateCenterMapOnUserOnStart calls setCenterMapOnUserOnStartUseCase`() = runTest {
        val enabled = true
        everySuspend { setCenterMapOnUserOnStartUseCase(enabled = enabled) } returns Unit

        viewModel.updateCenterMapOnUserOnStart(enabled = enabled)
        testDispatcher.scheduler.advanceUntilIdle()

        verifySuspend(mode = VerifyMode.exactly(1)) { setCenterMapOnUserOnStartUseCase(enabled = enabled) }
    }

    @Test
    fun `updateRestoreMapStateOnStart calls setRestoreMapStateOnStartUseCase`() = runTest {
        val enabled = true
        everySuspend { setRestoreMapStateOnStartUseCase(enabled = enabled) } returns Unit

        viewModel.updateRestoreMapStateOnStart(enabled = enabled)
        testDispatcher.scheduler.advanceUntilIdle()

        verifySuspend(mode = VerifyMode.exactly(1)) { setRestoreMapStateOnStartUseCase(enabled = enabled) }
    }

    @Test
    fun `updateShowReceiverLocations calls setShowReceiverLocationsUseCase`() = runTest {
        val enabled = true
        everySuspend { setShowReceiverLocationsUseCase(enabled = enabled) } returns Unit

        viewModel.updateShowReceiverLocations(enabled = enabled)
        testDispatcher.scheduler.advanceUntilIdle()

        verifySuspend(mode = VerifyMode.exactly(1)) { setShowReceiverLocationsUseCase(enabled = enabled) }
    }

    @Test
    fun `updateShowUserLocationOnMap calls setShowUserLocationOnMapUseCase`() = runTest {
        val enabled = true
        everySuspend { setShowUserLocationOnMapUseCase(enabled = enabled) } returns Unit

        viewModel.updateShowUserLocationOnMap(enabled = enabled)
        testDispatcher.scheduler.advanceUntilIdle()

        verifySuspend(mode = VerifyMode.exactly(1)) { setShowUserLocationOnMapUseCase(enabled = enabled) }
    }

    @Test
    fun `updateOpenUrlsExternally calls setOpenUrlsExternallyUseCase`() = runTest {
        val enabled = true
        everySuspend { setOpenUrlsExternallyUseCase(enabled = enabled) } returns Unit

        viewModel.updateOpenUrlsExternally(enabled = enabled)
        testDispatcher.scheduler.advanceUntilIdle()

        verifySuspend(mode = VerifyMode.exactly(1)) { setOpenUrlsExternallyUseCase(enabled = enabled) }
    }
}
