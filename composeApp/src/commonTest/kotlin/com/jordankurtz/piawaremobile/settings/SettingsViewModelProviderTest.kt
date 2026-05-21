package com.jordankurtz.piawaremobile.settings

import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.usecase.SettingsService
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
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

@ExperimentalCoroutinesApi
class SettingsViewModelProviderTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsService: SettingsService
    private lateinit var viewModel: SettingsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsService = mock()
        every { settingsService.loadSettings() } returns flowOf(Async.Success(Settings()))
        everySuspend { settingsService.setApiKey(any(), any()) } returns Unit
        everySuspend {
            settingsService.addCustomProvider(
                any(),
                any(),
                any(),
            )
        } returns Unit
        everySuspend { settingsService.deleteCustomProvider(any()) } returns Unit
        viewModel = SettingsViewModel(settingsService)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun updateApiKeyDelegatesToService() =
        runTest {
            viewModel.updateApiKey("stadia_toner", "abc123")
            testDispatcher.scheduler.advanceUntilIdle()
            verifySuspend { settingsService.setApiKey("stadia_toner", "abc123") }
        }

    @Test
    fun addCustomProviderDelegatesToService() =
        runTest {
            viewModel.addCustomProvider("my-id", "My Tiles", "https://example.com/{z}/{x}/{y}.png")
            testDispatcher.scheduler.advanceUntilIdle()
            verifySuspend {
                settingsService.addCustomProvider(
                    "my-id",
                    "My Tiles",
                    "https://example.com/{z}/{x}/{y}.png",
                )
            }
        }

    @Test
    fun deleteCustomProviderDelegatesToService() =
        runTest {
            viewModel.deleteCustomProvider("my-id")
            testDispatcher.scheduler.advanceUntilIdle()
            verifySuspend { settingsService.deleteCustomProvider("my-id") }
        }
}
