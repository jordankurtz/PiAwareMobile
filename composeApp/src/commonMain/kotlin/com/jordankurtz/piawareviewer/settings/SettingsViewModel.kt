package com.jordankurtz.piawareviewer.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jordankurtz.piawareviewer.extensions.stateIn
import com.jordankurtz.piawareviewer.model.Async
import com.jordankurtz.piawareviewer.settings.usecase.AddServerUseCase
import com.jordankurtz.piawareviewer.settings.usecase.LoadSettingsUseCase
import com.jordankurtz.piawareviewer.settings.usecase.SetRefreshIntervalUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    loadSettingsUseCase: LoadSettingsUseCase,
    private val addServerUseCase: AddServerUseCase,
    private val setRefreshIntervalUseCase: SetRefreshIntervalUseCase
) : ViewModel() {

    val settings: StateFlow<Async<Settings>>
        get() = _settings
    private val _settings = loadSettingsUseCase().stateIn(viewModelScope)

    fun addServer(name: String, address: String) = viewModelScope.launch {
        addServerUseCase(name, address)
    }

    fun updateRefreshInterval(refreshInterval: Int) = viewModelScope.launch {
        setRefreshIntervalUseCase(refreshInterval)
    }

}