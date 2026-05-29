package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.map.MapLibreStateController
import com.jordankurtz.piawaremobile.map.MapStateController
import com.jordankurtz.piawaremobile.map.TileProviderConfig
import com.jordankurtz.piawaremobile.map.TileProviders
import com.jordankurtz.piawaremobile.map.resolveActiveProviderConfig
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class MapModule {
    @Single
    fun provideActiveTileProviderConfigFlow(
        settingsRepository: SettingsRepository,
        applicationScope: CoroutineScope,
    ): StateFlow<TileProviderConfig> =
        settingsRepository.getSettings()
            .map { resolveActiveProviderConfig(it) }
            .stateIn(
                scope = applicationScope,
                started = SharingStarted.Eagerly,
                initialValue = TileProviders.DEFAULT,
            )

    @Factory
    fun provideMapStateController(): MapStateController = MapLibreStateController()
}
