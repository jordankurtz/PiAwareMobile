package com.jordankurtz.piawaremobile.settings

import app.cash.turbine.test
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.usecase.SettingsService
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
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
import kotlin.uuid.Uuid

@ExperimentalCoroutinesApi
class SettingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var settingsService: SettingsService
    private lateinit var viewModel: SettingsViewModel

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
        settingsService = mock()

        every { settingsService.loadSettings() } returns flowOf(Async.Success(settings))
        everySuspend { settingsService.addServer(any(), any(), any()) } returns Unit
        everySuspend { settingsService.editServer(any()) } returns Unit
        everySuspend { settingsService.deleteServer(any()) } returns Unit
        everySuspend { settingsService.setRefreshInterval(any()) } returns Unit
        everySuspend { settingsService.setCenterMapOnUserOnStart(any()) } returns Unit
        everySuspend { settingsService.setRestoreMapStateOnStart(any()) } returns Unit
        everySuspend { settingsService.setShowReceiverLocations(any()) } returns Unit
        everySuspend { settingsService.setShowUserLocationOnMap(any()) } returns Unit
        everySuspend { settingsService.setTrailDisplayMode(any()) } returns Unit
        everySuspend { settingsService.setShowMinimapTrails(any()) } returns Unit
        everySuspend { settingsService.setOpenUrlsExternally(any()) } returns Unit
        everySuspend { settingsService.setEnableFlightAwareApi(any()) } returns Unit
        everySuspend { settingsService.setFlightAwareApiKey(any()) } returns Unit

        viewModel = SettingsViewModel(settingsService)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `settings flow emits settings from service`() =
        runTest {
            val customSettings =
                settings.copy(
                    refreshInterval = 10,
                    centerMapOnUserOnStart = true,
                    enableFlightAwareApi = true,
                    flightAwareApiKey = "test_key",
                )
            every { settingsService.loadSettings() } returns flowOf(Async.Success(customSettings))

            val newViewModel = SettingsViewModel(settingsService)
            newViewModel.settings.test {
                assertEquals(Async.NotStarted, awaitItem())
                assertEquals(Async.Success(customSettings), awaitItem())
            }
        }

    @Test
    fun `addServer delegates to settings service`() =
        runTest {
            viewModel.addServer(
                name = "Test Server",
                address = "http://test.com",
                type = ServerType.PIAWARE,
            )
            testDispatcher.scheduler.advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) {
                settingsService.addServer("Test Server", "http://test.com", ServerType.PIAWARE)
            }
        }

    @Test
    fun `addServer with readsb type delegates to settings service`() =
        runTest {
            viewModel.addServer(
                name = "Readsb Server",
                address = "http://readsb.local",
                type = ServerType.READSB,
            )
            testDispatcher.scheduler.advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) {
                settingsService.addServer("Readsb Server", "http://readsb.local", ServerType.READSB)
            }
        }

    @Test
    fun `editServer delegates to settings service`() =
        runTest {
            val server = Server(name = "Edited", address = "http://edited.com")
            viewModel.editServer(server)
            testDispatcher.scheduler.advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) {
                settingsService.editServer(server)
            }
        }

    @Test
    fun `deleteServer delegates to settings service`() =
        runTest {
            val id = Uuid.random()
            viewModel.deleteServer(id)
            testDispatcher.scheduler.advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) {
                settingsService.deleteServer(id)
            }
        }

    @Test
    fun `updateRefreshInterval delegates to settings service`() =
        runTest {
            viewModel.updateRefreshInterval(15)
            testDispatcher.scheduler.advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) { settingsService.setRefreshInterval(15) }
        }

    @Test
    fun `updateCenterMapOnUserOnStart delegates to settings service`() =
        runTest {
            viewModel.updateCenterMapOnUserOnStart(true)
            testDispatcher.scheduler.advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) { settingsService.setCenterMapOnUserOnStart(true) }
        }

    @Test
    fun `updateRestoreMapStateOnStart delegates to settings service`() =
        runTest {
            viewModel.updateRestoreMapStateOnStart(true)
            testDispatcher.scheduler.advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) { settingsService.setRestoreMapStateOnStart(true) }
        }

    @Test
    fun `updateShowReceiverLocations delegates to settings service`() =
        runTest {
            viewModel.updateShowReceiverLocations(true)
            testDispatcher.scheduler.advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) { settingsService.setShowReceiverLocations(true) }
        }

    @Test
    fun `updateShowUserLocationOnMap delegates to settings service`() =
        runTest {
            viewModel.updateShowUserLocationOnMap(true)
            testDispatcher.scheduler.advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) { settingsService.setShowUserLocationOnMap(true) }
        }

    @Test
    fun `updateOpenUrlsExternally delegates to settings service`() =
        runTest {
            viewModel.updateOpenUrlsExternally(true)
            testDispatcher.scheduler.advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) { settingsService.setOpenUrlsExternally(true) }
        }
}
