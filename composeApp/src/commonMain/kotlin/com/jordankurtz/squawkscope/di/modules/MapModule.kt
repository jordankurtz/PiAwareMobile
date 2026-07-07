package com.jordankurtz.squawkscope.di.modules

import com.jordankurtz.squawkscope.di.annotations.IODispatcher
import com.jordankurtz.squawkscope.map.MapComposeStateController
import com.jordankurtz.squawkscope.map.MapStateController
import com.jordankurtz.squawkscope.map.TileProviderConfig
import com.jordankurtz.squawkscope.map.resolveActiveProviderConfig
import com.jordankurtz.squawkscope.settings.repo.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class MapModule {
    @Single
    fun provideActiveTileProviderConfigFlow(
        settingsRepository: SettingsRepository,
        applicationScope: CoroutineScope,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): StateFlow<TileProviderConfig> {
        val initial =
            runBlocking(ioDispatcher) {
                resolveActiveProviderConfig(settingsRepository.getSettings().first())
            }
        return settingsRepository.getSettings()
            .map { resolveActiveProviderConfig(it) }
            .stateIn(scope = applicationScope, started = SharingStarted.Eagerly, initialValue = initial)
    }

    @Factory
    fun provideMapStateController(): MapStateController = MapComposeStateController()
}
