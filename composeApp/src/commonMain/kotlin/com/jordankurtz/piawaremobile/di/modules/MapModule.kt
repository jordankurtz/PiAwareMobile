package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.TileProviderConfig
import com.jordankurtz.piawaremobile.map.TileProviders
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class MapModule {
    @Single
    fun provideActiveTileProviderConfigFlow(
        settingsRepository: SettingsRepository,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): StateFlow<TileProviderConfig> {
        val scope = CoroutineScope(ioDispatcher + SupervisorJob())
        val initial =
            runBlocking(ioDispatcher) {
                TileProviders.findById(settingsRepository.getSettings().first().mapProviderId)
            }
        return settingsRepository.getSettings()
            .map { TileProviders.findById(it.mapProviderId) }
            .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = initial)
    }
}
