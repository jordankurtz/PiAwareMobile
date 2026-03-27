package com.jordankurtz.piawaremobile.settings.usecase

import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.ServerType
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.TrailDisplayMode
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.impl.SettingsServiceImpl
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.capture.Capture.Companion.slot
import dev.mokkery.matcher.capture.SlotCapture
import dev.mokkery.matcher.capture.capture
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.uuid.Uuid

class SettingsServiceImplTest {
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var settingsService: SettingsServiceImpl
    private val testDispatcher = StandardTestDispatcher()

    private val defaultSettings =
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

    private val serverId1 = Uuid.parse("00000000-0000-0000-0000-000000000001")
    private val serverId2 = Uuid.parse("00000000-0000-0000-0000-000000000002")

    @BeforeTest
    fun setUp() {
        settingsRepository = mock()
        settingsService = SettingsServiceImpl(settingsRepository, testDispatcher)
    }

    private fun mockSettings(settings: Settings = defaultSettings) {
        everySuspend { settingsRepository.getSettings() } returns flowOf(settings)
        everySuspend { settingsRepository.saveSettings(any()) } returns Unit
    }

    private fun captureSettings(): SlotCapture<Settings> {
        val slot = slot<Settings>()
        everySuspend { settingsRepository.saveSettings(capture(slot)) } returns Unit
        return slot
    }

    // Server management tests

    @Test
    fun `addServer appends server to list with default piaware type`() =
        runTest(testDispatcher) {
            mockSettings()
            val slot = captureSettings()

            settingsService.addServer("Test Server", "http://192.168.1.100")

            verifySuspend(VerifyMode.exactly(1)) { settingsRepository.saveSettings(any()) }
            val saved = (slot.value as SlotCapture.Value.Present).value
            assertEquals(1, saved.servers.size)
            assertEquals("Test Server", saved.servers[0].name)
            assertEquals("http://192.168.1.100", saved.servers[0].address)
            assertEquals(ServerType.PIAWARE, saved.servers[0].type)
        }

    @Test
    fun `addServer with readsb type saves correct server type`() =
        runTest(testDispatcher) {
            mockSettings()
            val slot = captureSettings()

            settingsService.addServer("Readsb Server", "http://192.168.1.200", ServerType.READSB)

            verifySuspend(VerifyMode.exactly(1)) { settingsRepository.saveSettings(any()) }
            val saved = (slot.value as SlotCapture.Value.Present).value
            assertEquals(1, saved.servers.size)
            assertEquals("Readsb Server", saved.servers[0].name)
            assertEquals(ServerType.READSB, saved.servers[0].type)
        }

    @Test
    fun `editServer updates the correct server and preserves others`() =
        runTest(testDispatcher) {
            val server1 = Server(id = serverId1, name = "Server 1", address = "host1.local")
            val server2 = Server(id = serverId2, name = "Server 2", address = "host2.local")
            mockSettings(defaultSettings.copy(servers = listOf(server1, server2)))
            val slot = captureSettings()

            settingsService.editServer(Server(id = serverId1, name = "Renamed", address = "new.local"))

            val saved = (slot.value as SlotCapture.Value.Present).value
            assertEquals(2, saved.servers.size)
            assertEquals("Renamed", saved.servers[0].name)
            assertEquals("new.local", saved.servers[0].address)
            assertEquals("Server 2", saved.servers[1].name)
        }

    @Test
    fun `editServer with non-existent ID leaves list unchanged`() =
        runTest(testDispatcher) {
            val server1 = Server(id = serverId1, name = "Server 1", address = "host1.local")
            mockSettings(defaultSettings.copy(servers = listOf(server1)))
            val slot = captureSettings()

            val unknownId = Uuid.parse("00000000-0000-0000-0000-000000000099")
            settingsService.editServer(Server(id = unknownId, name = "Ghost", address = "ghost.local"))

            val saved = (slot.value as SlotCapture.Value.Present).value
            assertEquals(1, saved.servers.size)
            assertEquals("Server 1", saved.servers[0].name)
        }

    @Test
    fun `deleteServer removes only matching server`() =
        runTest(testDispatcher) {
            val server1 = Server(id = serverId1, name = "Server 1", address = "host1.local")
            val server2 = Server(id = serverId2, name = "Server 2", address = "host2.local")
            mockSettings(defaultSettings.copy(servers = listOf(server1, server2)))
            val slot = captureSettings()

            settingsService.deleteServer(serverId1)

            val saved = (slot.value as SlotCapture.Value.Present).value
            assertEquals(1, saved.servers.size)
            assertEquals("Server 2", saved.servers[0].name)
        }

    @Test
    fun `deleteServer with non-existent ID leaves list unchanged`() =
        runTest(testDispatcher) {
            val server1 = Server(id = serverId1, name = "Server 1", address = "host1.local")
            mockSettings(defaultSettings.copy(servers = listOf(server1)))
            val slot = captureSettings()

            val unknownId = Uuid.parse("00000000-0000-0000-0000-000000000099")
            settingsService.deleteServer(unknownId)

            val saved = (slot.value as SlotCapture.Value.Present).value
            assertEquals(1, saved.servers.size)
            assertEquals("Server 1", saved.servers[0].name)
        }

    // Settings update tests

    @Test
    fun `setRefreshInterval saves updated interval`() =
        runTest(testDispatcher) {
            mockSettings()
            val slot = captureSettings()

            settingsService.setRefreshInterval(10)

            val saved = (slot.value as SlotCapture.Value.Present).value
            assertEquals(10, saved.refreshInterval)
        }

    @Test
    fun `setCenterMapOnUserOnStart saves updated value`() =
        runTest(testDispatcher) {
            mockSettings()
            val slot = captureSettings()

            settingsService.setCenterMapOnUserOnStart(true)

            val saved = (slot.value as SlotCapture.Value.Present).value
            assertEquals(true, saved.centerMapOnUserOnStart)
        }

    @Test
    fun `setRestoreMapStateOnStart saves updated value`() =
        runTest(testDispatcher) {
            mockSettings()
            val slot = captureSettings()

            settingsService.setRestoreMapStateOnStart(true)

            val saved = (slot.value as SlotCapture.Value.Present).value
            assertEquals(true, saved.restoreMapStateOnStart)
        }

    @Test
    fun `setShowReceiverLocations saves updated value`() =
        runTest(testDispatcher) {
            mockSettings()
            val slot = captureSettings()

            settingsService.setShowReceiverLocations(true)

            val saved = (slot.value as SlotCapture.Value.Present).value
            assertEquals(true, saved.showReceiverLocations)
        }

    @Test
    fun `setShowUserLocationOnMap saves updated value`() =
        runTest(testDispatcher) {
            mockSettings()
            val slot = captureSettings()

            settingsService.setShowUserLocationOnMap(true)

            val saved = (slot.value as SlotCapture.Value.Present).value
            assertEquals(true, saved.showUserLocationOnMap)
        }

    @Test
    fun `setOpenUrlsExternally saves updated value`() =
        runTest(testDispatcher) {
            mockSettings()
            val slot = captureSettings()

            settingsService.setOpenUrlsExternally(true)

            val saved = (slot.value as SlotCapture.Value.Present).value
            assertEquals(true, saved.openUrlsExternally)
        }

    @Test
    fun `setEnableFlightAwareApi saves updated value`() =
        runTest(testDispatcher) {
            mockSettings()
            val slot = captureSettings()

            settingsService.setEnableFlightAwareApi(true)

            val saved = (slot.value as SlotCapture.Value.Present).value
            assertEquals(true, saved.enableFlightAwareApi)
        }

    @Test
    fun `setFlightAwareApiKey saves updated key`() =
        runTest(testDispatcher) {
            mockSettings()
            val slot = captureSettings()

            settingsService.setFlightAwareApiKey("my-api-key")

            val saved = (slot.value as SlotCapture.Value.Present).value
            assertEquals("my-api-key", saved.flightAwareApiKey)
        }

    @Test
    fun `setTrailDisplayMode delegates to repository`() =
        runTest(testDispatcher) {
            everySuspend { settingsRepository.setTrailDisplayMode(any()) } returns Unit

            settingsService.setTrailDisplayMode(TrailDisplayMode.ALL)

            verifySuspend(VerifyMode.exactly(1)) {
                settingsRepository.setTrailDisplayMode(TrailDisplayMode.ALL)
            }
        }

    @Test
    fun `setShowMinimapTrails delegates to repository`() =
        runTest(testDispatcher) {
            everySuspend { settingsRepository.setShowMinimapTrails(any()) } returns Unit

            settingsService.setShowMinimapTrails(true)

            verifySuspend(VerifyMode.exactly(1)) {
                settingsRepository.setShowMinimapTrails(true)
            }
        }

    // Getter tests

    @Test
    fun `getFlightAwareApiKey returns key from settings`() =
        runTest(testDispatcher) {
            mockSettings(defaultSettings.copy(flightAwareApiKey = "test-key"))

            val result = settingsService.getFlightAwareApiKey()

            assertEquals("test-key", result)
        }

    @Test
    fun `loadSettings returns async flow`() =
        runTest(testDispatcher) {
            everySuspend { settingsRepository.getSettings() } returns flowOf(defaultSettings)

            val flow = settingsService.loadSettings()
            assertNotNull(flow)
        }
}
