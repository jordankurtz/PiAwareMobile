package com.jordankurtz.piawaremobile.map.offline

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.Single

@Single
class DownloadScopeHolder(
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
)
