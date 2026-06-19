package com.jordankurtz.piawaremobile.aircraft.cache

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jordankurtz.piawaremobile.aircraft.usecase.PrefetchFlightsUseCase
import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.offline.BoundingBox
import com.jordankurtz.piawaremobile.model.Async
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory

@Factory
class FlightCacheViewModel(
    private val prefetchFlightsUseCase: PrefetchFlightsUseCase,
    private val flightCacheRepo: FlightCacheRepo,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _regions = MutableStateFlow<List<CachedRegion>>(emptyList())
    val regions: StateFlow<List<CachedRegion>> = _regions.asStateFlow()

    private val _prefetchState = MutableStateFlow<Async<Int>>(Async.NotStarted)
    val prefetchState: StateFlow<Async<Int>> = _prefetchState.asStateFlow()

    init {
        loadRegions()
    }

    private fun loadRegions() {
        viewModelScope.launch(ioDispatcher) {
            _regions.value = flightCacheRepo.getAllRegions()
        }
    }

    fun startPrefetch(
        name: String,
        box: BoundingBox,
        daysAhead: Int,
    ) {
        viewModelScope.launch(ioDispatcher) {
            _prefetchState.value = Async.Loading
            _prefetchState.value = prefetchFlightsUseCase(name, box, daysAhead)
            loadRegions()
        }
    }

    fun deleteRegion(regionId: Long) {
        viewModelScope.launch(ioDispatcher) {
            flightCacheRepo.deleteRegion(regionId)
            loadRegions()
        }
    }

    fun resetPrefetchState() {
        _prefetchState.value = Async.NotStarted
    }
}
