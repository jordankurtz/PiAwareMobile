package com.jordankurtz.piawaremobile.location

import com.jordankurtz.piawaremobile.di.modules.ContextWrapper
import com.jordankurtz.piawaremobile.model.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Factory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

@Factory(binds = [LocationService::class])
actual class LocationServiceImpl actual constructor(private val contextWrapper: ContextWrapper) :
    LocationService {
    private var updateJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true }

    actual override fun startLocationUpdates(onLocationUpdate: (Location) -> Unit) {
        updateJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val location = fetchLocationFromIP()
                    location?.let { onLocationUpdate(it) }
                    delay(10000) // Update every 10 seconds
                } catch (e: Exception) {
                    println("Error fetching location: ${e.message}")
                    delay(10000)
                }
            }
        }
    }

    actual override fun stopLocationUpdates() {
        updateJob?.cancel()
        updateJob = null
    }

    actual override fun requestPermissions(onResult: (Boolean) -> Unit) {
        // Desktop doesn't need permission for IP-based location
        onResult(true)
    }

    private fun fetchLocationFromIP(): Location? {
        return try {
            // Using ip-api.com free API for geolocation
            val url = URL("http://ip-api.com/json/")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val response = BufferedReader(InputStreamReader(connection.inputStream))
                .use { it.readText() }

            val locationData = json.decodeFromString<IPLocationResponse>(response)

            if (locationData.status == "success") {
                Location(
                    latitude = locationData.lat,
                    longitude = locationData.lon
                )
            } else {
                null
            }
        } catch (e: Exception) {
            println("Failed to fetch IP location: ${e.message}")
            null
        }
    }
}

@Serializable
data class IPLocationResponse(
    val status: String,
    val lat: Double,
    val lon: Double
)
