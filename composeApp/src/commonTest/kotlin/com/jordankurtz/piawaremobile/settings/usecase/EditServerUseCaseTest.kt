package com.jordankurtz.piawaremobile.settings.usecase

import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.impl.EditServerUseCaseImpl
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
import kotlin.uuid.Uuid

class EditServerUseCaseTest {
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var editServerUseCase: EditServerUseCase
    private val testDispatcher = StandardTestDispatcher()

    private val serverId1 = Uuid.parse("00000000-0000-0000-0000-000000000001")
    private val serverId2 = Uuid.parse("00000000-0000-0000-0000-000000000002")

    @BeforeTest
    fun setUp() {
        settingsRepository = mock()
        editServerUseCase = EditServerUseCaseImpl(settingsRepository, testDispatcher)
    }

    @Test
    fun `edit updates the correct server and preserves others`() =
        runTest(testDispatcher) {
            val server1 = Server(id = serverId1, name = "Server 1", address = "host1.local")
            val server2 = Server(id = serverId2, name = "Server 2", address = "host2.local")
            val settings = Settings(servers = listOf(server1, server2))

            everySuspend { settingsRepository.getSettings() } returns flowOf(settings)
            val settingsSlot = slot<Settings>()
            everySuspend { settingsRepository.saveSettings(capture(settingsSlot)) } returns Unit

            val updatedServer = Server(id = serverId1, name = "Renamed", address = "new.local")
            editServerUseCase(updatedServer)

            verifySuspend(VerifyMode.exactly(1)) { settingsRepository.saveSettings(any()) }

            val saved = (settingsSlot.value as SlotCapture.Value.Present).value
            assertEquals(2, saved.servers.size)
            assertEquals("Renamed", saved.servers[0].name)
            assertEquals("new.local", saved.servers[0].address)
            assertEquals("Server 2", saved.servers[1].name)
        }

    @Test
    fun `edit with non-existent ID leaves list unchanged`() =
        runTest(testDispatcher) {
            val server1 = Server(id = serverId1, name = "Server 1", address = "host1.local")
            val settings = Settings(servers = listOf(server1))

            everySuspend { settingsRepository.getSettings() } returns flowOf(settings)
            val settingsSlot = slot<Settings>()
            everySuspend { settingsRepository.saveSettings(capture(settingsSlot)) } returns Unit

            val unknownId = Uuid.parse("00000000-0000-0000-0000-000000000099")
            editServerUseCase(Server(id = unknownId, name = "Ghost", address = "ghost.local"))

            val saved = (settingsSlot.value as SlotCapture.Value.Present).value
            assertEquals(1, saved.servers.size)
            assertEquals("Server 1", saved.servers[0].name)
        }
}
