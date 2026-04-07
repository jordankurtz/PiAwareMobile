package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.TileProviderConfig
import com.jordankurtz.piawaremobile.map.TileProviders
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class MapModule {
    @Single
    fun provideActiveTileProviderConfig(
        settingsRepository: SettingsRepository,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): TileProviderConfig =
        runBlocking(ioDispatcher) {
            TileProviders.findById(settingsRepository.getSettings().first().mapProviderId)
        }
}
