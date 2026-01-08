package com.jordankurtz.piawaremobile.settings.usecase

import com.jordankurtz.piawaremobile.settings.TrailDisplayMode
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.impl.SetTrailDisplayModeUseCaseImpl
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class SetTrailDisplayModeUseCaseTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: SetTrailDisplayModeUseCase

    @BeforeTest
    fun setUp() {
        settingsRepository = mock()
        useCase = SetTrailDisplayModeUseCaseImpl(settingsRepository)
    }

    @Test
    fun `invoke with ALL should call repository with ALL`() = runTest {
        everySuspend { settingsRepository.setTrailDisplayMode(TrailDisplayMode.ALL) } returns Unit

        useCase(TrailDisplayMode.ALL)

        verifySuspend { settingsRepository.setTrailDisplayMode(TrailDisplayMode.ALL) }
    }

    @Test
    fun `invoke with SELECTED should call repository with SELECTED`() = runTest {
        everySuspend { settingsRepository.setTrailDisplayMode(TrailDisplayMode.SELECTED) } returns Unit

        useCase(TrailDisplayMode.SELECTED)

        verifySuspend { settingsRepository.setTrailDisplayMode(TrailDisplayMode.SELECTED) }
    }

    @Test
    fun `invoke with NONE should call repository with NONE`() = runTest {
        everySuspend { settingsRepository.setTrailDisplayMode(TrailDisplayMode.NONE) } returns Unit

        useCase(TrailDisplayMode.NONE)

        verifySuspend { settingsRepository.setTrailDisplayMode(TrailDisplayMode.NONE) }
    }
}
