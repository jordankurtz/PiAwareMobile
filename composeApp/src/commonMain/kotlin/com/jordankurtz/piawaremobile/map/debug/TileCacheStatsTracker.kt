package com.jordankurtz.piawaremobile.map.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

@Single
class TileCacheStatsTracker {
    private val _stats = MutableStateFlow(TileCacheStats())
    val stats: StateFlow<TileCacheStats> = _stats.asStateFlow()

    fun recordDiskHit() {
        _stats.update { it.copy(diskHits = it.diskHits + 1) }
    }

    fun recordNetworkFetch() {
        _stats.update { it.copy(networkFetches = it.networkFetches + 1) }
    }

    fun recordError() {
        _stats.update { it.copy(errors = it.errors + 1) }
    }
}
