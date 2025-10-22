package com.jordankurtz.piawaremobile.settings

data class Settings (
    val servers: List<Server>,
    val refreshInterval: Int,
    val centerMapOnUserOnStart: Boolean,
    val restoreMapStateOnStart: Boolean,
    val showReceiverLocations: Boolean,
    val showUserLocationOnMap: Boolean
)
