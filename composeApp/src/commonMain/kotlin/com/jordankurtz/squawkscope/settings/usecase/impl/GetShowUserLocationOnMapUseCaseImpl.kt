package com.jordankurtz.squawkscope.settings.usecase.impl

import com.jordankurtz.squawkscope.settings.usecase.GetShowUserLocationOnMapUseCase
import com.jordankurtz.squawkscope.settings.usecase.SettingsService
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory(binds = [GetShowUserLocationOnMapUseCase::class])
class GetShowUserLocationOnMapUseCaseImpl(
    private val settingsService: SettingsService,
) : GetShowUserLocationOnMapUseCase {
    override fun invoke(): Flow<Boolean> {
        return settingsService.getShowUserLocationOnMap()
    }
}
