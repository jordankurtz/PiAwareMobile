package com.jordankurtz.squawkscope.map.offline

import com.jordankurtz.squawkscope.di.annotations.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.annotation.Single

@Single
class DownloadScopeHolder(
    @IODispatcher ioDispatcher: CoroutineDispatcher,
) {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    fun close() {
        scope.cancel()
    }
}
