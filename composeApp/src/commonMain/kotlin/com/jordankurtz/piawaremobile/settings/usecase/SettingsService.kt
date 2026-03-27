package com.jordankurtz.piawaremobile.settings.usecase

import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.ServerType
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.TrailDisplayMode
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Suppress("TooManyFunctions")
interface SettingsService {
    fun loadSettings(): Flow<Async<Settings>>

    fun getShowUserLocationOnMap(): Flow<Boolean>

    suspend fun getFlightAwareApiKey(): String

    suspend fun addServer(
        name: String,
        address: String,
        type: ServerType = ServerType.PIAWARE,
    )

    suspend fun editServer(server: Server)

    suspend fun deleteServer(id: Uuid)

    suspend fun setRefreshInterval(interval: Int)

    suspend fun setCenterMapOnUserOnStart(enabled: Boolean)

    suspend fun setRestoreMapStateOnStart(enabled: Boolean)

    suspend fun setShowReceiverLocations(enabled: Boolean)

    suspend fun setShowUserLocationOnMap(enabled: Boolean)

    suspend fun setTrailDisplayMode(trailDisplayMode: TrailDisplayMode)

    suspend fun setShowMinimapTrails(enabled: Boolean)

    suspend fun setOpenUrlsExternally(enabled: Boolean)

    suspend fun setEnableFlightAwareApi(enabled: Boolean)

    suspend fun setFlightAwareApiKey(apiKey: String)
}
