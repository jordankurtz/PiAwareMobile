package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SettingsService
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory(binds = [LoadSettingsUseCase::class])
class LoadSettingsUseCaseImpl(
    private val settingsService: SettingsService,
) : LoadSettingsUseCase {
    override operator fun invoke(): Flow<Async<Settings>> {
        return settingsService.loadSettings()
    }
}
