package com.jordankurtz.squawkscope.settings.usecase.impl

import com.jordankurtz.squawkscope.model.Async
import com.jordankurtz.squawkscope.settings.Settings
import com.jordankurtz.squawkscope.settings.usecase.LoadSettingsUseCase
import com.jordankurtz.squawkscope.settings.usecase.SettingsService
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory(binds = [LoadSettingsUseCase::class])
class LoadSettingsUseCaseImpl(
    private val settingsService: SettingsService,
) : LoadSettingsUseCase {
    override operator fun invoke(): Flow<Async<Settings>> = settingsService.loadSettings()
}
