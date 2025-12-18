package com.jordankurtz.piawaremobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Aircraft (
    val hex: String,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val flight: String? = null,
    val track: Float? = null,
    val squawk: String? = null,
    val category: String? = null,
    @SerialName("alt_baro")
    val altBaro: String? = null,
    @SerialName("alt_geom")
    val altGeom: Int? = null,
    val gs: Float? = null,
    @SerialName("baro_rate")
    val baroRate: Int? = null,
    @SerialName("nav_qnh")
    val navQnh: Float? = null,
    @SerialName("nav_altitude_mcp")
    val navAltitudeMcp: Int? = null,
    @SerialName("nav_heading")
    val navHeading: Float? = null,
    val version: Int? = null,
    val nic: Int? = null,
    val rc: Int? = null,
    @SerialName("seen_pos")
    val seenPos: Float? = null,
    @SerialName("nic_baro")
    val nicBaro: Int? = null,
    @SerialName("nac_p")
    val nacP: Int? = null,
    @SerialName("nac_v")
    val nacV: Int? = null,
    val sil: Int? = null,
    @SerialName("sil_type")
    val silType: String? = null,
    val messages: Int? = null,
    val seen: Float? = null,
    val rssi: Float? = null
)
