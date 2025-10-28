package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.AddServerUseCase
import kotlinx.coroutines.flow.first

class AddServerUseCaseImpl(private val settingsRepository: SettingsRepository) : AddServerUseCase {
    override suspend operator fun invoke(name: String, address: String) {
        val settings = settingsRepository.getSettings().first()
        val currentServers = settings.servers
        settingsRepository.saveSettings(
            settings.copy(
                servers = currentServers + Server(
                    name = name,
                    address = address
                )
            )
        )
    }
}
