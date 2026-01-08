package com.jordankurtz.piawaremobile.settings.usecase

import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.impl.SetShowMinimapTrailsUseCaseImpl
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class SetShowMinimapTrailsUseCaseTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: SetShowMinimapTrailsUseCase

    @BeforeTest
    fun setUp() {
        settingsRepository = mock()
        useCase = SetShowMinimapTrailsUseCaseImpl(settingsRepository)
    }

    @Test
    fun `invoke with true should call repository with true`() = runTest {
        everySuspend { settingsRepository.setShowMinimapTrails(true) } returns Unit

        useCase(true)

        verifySuspend { settingsRepository.setShowMinimapTrails(true) }
    }

    @Test
    fun `invoke with false should call repository with false`() = runTest {
        everySuspend { settingsRepository.setShowMinimapTrails(false) } returns Unit

        useCase(false)

        verifySuspend { settingsRepository.setShowMinimapTrails(false) }
    }
}
