package com.jordankurtz.piawaremobile.settings.usecase

import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import kotlinx.coroutines.flow.first

class AddServerUseCase(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(name: String, address: String) {
        val settings = settingsRepository.getSettings().first()
        val currentServers = settings.servers
        settingsRepository.saveSettings(settings.copy(servers = currentServers + Server(name, address)))
    }
}