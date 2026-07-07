package com.jordankurtz.squawkscope.aircraft.api

import com.jordankurtz.squawkscope.settings.ServerType

interface AircraftDataSourceFactory {
    fun getDataSource(serverType: ServerType): AircraftDataSource
}
