package com.jordankurtz.squawkscope.aircraft.api.impl

import com.jordankurtz.squawkscope.aircraft.api.AircraftDataSource
import com.jordankurtz.squawkscope.aircraft.api.AircraftDataSourceFactory
import com.jordankurtz.squawkscope.settings.ServerType
import org.koin.core.annotation.Single

@Single(binds = [AircraftDataSourceFactory::class])
class AircraftDataSourceFactoryImpl(
    private val piAwareDataSource: PiAwareDataSource,
    private val readsbDataSource: ReadsbDataSource,
) : AircraftDataSourceFactory {
    override fun getDataSource(serverType: ServerType): AircraftDataSource =
        when (serverType) {
            ServerType.PIAWARE -> piAwareDataSource
            ServerType.READSB -> readsbDataSource
        }
}
