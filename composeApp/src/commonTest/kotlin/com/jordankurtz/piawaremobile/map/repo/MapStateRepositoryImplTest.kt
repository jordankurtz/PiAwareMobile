package com.jordankurtz.piawaremobile.map.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.jordankurtz.piawaremobile.model.MapState
import com.jordankurtz.piawaremobile.testutil.FakeDataStore
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MapStateRepositoryImplTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: MapStateRepository

    @BeforeTest
    fun setUp() {
        dataStore = FakeDataStore()
        repository = MapStateRepositoryImpl(dataStore)
    }

    @Test
    fun `saveMapState should store the map state`() =
        runTest {
            val latitude = 0.1
            val longitude = 0.2
            val zoom = 5.0
            repository.saveMapState(latitude, longitude, zoom)

            val savedState = repository.getSavedMapState()

            assertEquals(latitude, savedState.latitude)
            assertEquals(longitude, savedState.longitude)
            assertEquals(zoom, savedState.zoom)
        }

    @Test
    fun `getSavedMapState should return default state when nothing is saved`() =
        runTest {
            val savedState = repository.getSavedMapState()

            assertEquals(MapStateRepositoryImpl.DEFAULT_STATE, savedState)
        }

    @Test
    fun `getSavedMapState should return saved state`() =
        runTest {
            // Given
            val latitude = 0.3
            val longitude = 0.4
            val zoom = 6.0
            repository.saveMapState(latitude, longitude, zoom)

            // When
            val result = repository.getSavedMapState()

            // Then
            assertEquals(MapState(latitude = latitude, longitude = longitude, zoom = zoom), result)
        }
}
