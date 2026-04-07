package com.jordankurtz.piawaremobile.map.offline

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.Single

@Single
class DownloadScopeHolder(
    @IODispatcher ioDispatcher: CoroutineDispatcher,
) {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
}
