package com.jordankurtz.piawareviewer.settings.usecase

import com.jordankurtz.piawareviewer.settings.Server
import com.jordankurtz.piawareviewer.settings.repo.SettingsRepository
import kotlinx.coroutines.flow.first

class AddServerUseCase(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(name: String, address: String) {
        val settings = settingsRepository.getSettings().first()
        val currentServers = settings.servers
        settingsRepository.saveSettings(settings.copy(servers = currentServers + Server(name, address)))
    }
}