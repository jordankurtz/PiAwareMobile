package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.EditServerUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory(binds = [EditServerUseCase::class])
class EditServerUseCaseImpl(
    private val settingsRepository: SettingsRepository,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : EditServerUseCase {
    override suspend operator fun invoke(server: Server) {
        withContext(ioDispatcher) {
            val settings = settingsRepository.getSettings().first()
            settingsRepository.saveSettings(
                settings.copy(
                    servers = settings.servers.map { if (it.id == server.id) server else it },
                ),
            )
        }
    }
}
