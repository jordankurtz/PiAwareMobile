package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.extensions.async
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.ServerType
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.TrailDisplayMode
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.SettingsService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("TooManyFunctions")
@Single(binds = [SettingsService::class])
class SettingsServiceImpl(
    private val settingsRepository: SettingsRepository,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : SettingsService {
    override fun loadSettings(): Flow<Async<Settings>> {
        return settingsRepository.getSettings()
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
            .async()
    }

    override fun getShowUserLocationOnMap(): Flow<Boolean> {
        return settingsRepository.getSettings().map { it.showUserLocationOnMap }
    }

    override suspend fun getFlightAwareApiKey(): String {
        return settingsRepository.getSettings().first().flightAwareApiKey
    }

    override suspend fun addServer(
        name: String,
        address: String,
        type: ServerType,
    ) {
        withContext(ioDispatcher) {
            val settings = settingsRepository.getSettings().first()
            settingsRepository.saveSettings(
                settings.copy(
                    servers = settings.servers + Server(name = name, address = address, type = type),
                ),
            )
        }
    }

    override suspend fun editServer(server: Server) {
        withContext(ioDispatcher) {
            val settings = settingsRepository.getSettings().first()
            settingsRepository.saveSettings(
                settings.copy(
                    servers = settings.servers.map { if (it.id == server.id) server else it },
                ),
            )
        }
    }

    override suspend fun deleteServer(id: Uuid) {
        withContext(ioDispatcher) {
            val settings = settingsRepository.getSettings().first()
            settingsRepository.saveSettings(
                settings.copy(
                    servers = settings.servers.filter { it.id != id },
                ),
            )
        }
    }

    private suspend fun updateSetting(transform: (Settings) -> Settings) {
        withContext(ioDispatcher) {
            val settings = settingsRepository.getSettings().first()
            settingsRepository.saveSettings(transform(settings))
        }
    }

    override suspend fun setRefreshInterval(interval: Int) = updateSetting { it.copy(refreshInterval = interval) }

    override suspend fun setCenterMapOnUserOnStart(enabled: Boolean) =
        updateSetting { it.copy(centerMapOnUserOnStart = enabled) }

    override suspend fun setRestoreMapStateOnStart(enabled: Boolean) =
        updateSetting { it.copy(restoreMapStateOnStart = enabled) }

    override suspend fun setShowReceiverLocations(enabled: Boolean) =
        updateSetting { it.copy(showReceiverLocations = enabled) }

    override suspend fun setShowUserLocationOnMap(enabled: Boolean) =
        updateSetting { it.copy(showUserLocationOnMap = enabled) }

    override suspend fun setTrailDisplayMode(trailDisplayMode: TrailDisplayMode) {
        settingsRepository.setTrailDisplayMode(trailDisplayMode)
    }

    override suspend fun setShowMinimapTrails(enabled: Boolean) {
        settingsRepository.setShowMinimapTrails(enabled)
    }

    override suspend fun setOpenUrlsExternally(enabled: Boolean) =
        updateSetting { it.copy(openUrlsExternally = enabled) }

    override suspend fun setEnableFlightAwareApi(enabled: Boolean) =
        updateSetting { it.copy(enableFlightAwareApi = enabled) }

    override suspend fun setFlightAwareApiKey(apiKey: String) = updateSetting { it.copy(flightAwareApiKey = apiKey) }

    override suspend fun setMapProviderId(providerId: String) = updateSetting { it.copy(mapProviderId = providerId) }
}
