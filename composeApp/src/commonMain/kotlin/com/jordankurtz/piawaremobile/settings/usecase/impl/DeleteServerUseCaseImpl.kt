package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.DeleteServerUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import kotlin.uuid.Uuid

@Factory(binds = [DeleteServerUseCase::class])
class DeleteServerUseCaseImpl(
    private val settingsRepository: SettingsRepository,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : DeleteServerUseCase {
    override suspend operator fun invoke(id: Uuid) {
        withContext(ioDispatcher) {
            val settings = settingsRepository.getSettings().first()
            settingsRepository.saveSettings(
                settings.copy(
                    servers = settings.servers.filter { it.id != id },
                ),
            )
        }
    }
}
