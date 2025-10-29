package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.AddServerUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory(binds = [AddServerUseCase::class])
class AddServerUseCaseImpl(
    private val settingsRepository: SettingsRepository,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher
) : AddServerUseCase {
    override suspend operator fun invoke(name: String, address: String) {
        withContext(ioDispatcher) {
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
}
