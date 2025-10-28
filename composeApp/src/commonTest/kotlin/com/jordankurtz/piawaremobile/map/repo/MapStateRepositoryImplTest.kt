package com.jordankurtz.piawaremobile.map.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.jordankurtz.piawaremobile.model.MapState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MapStateRepositoryImplTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: MapStateRepository

    @BeforeTest
    fun setUp() {
        dataStore = FakeDataStore()
        repository = MapStateRepositoryImpl(dataStore)
    }

    @Test
    fun `saveMapState should store the map state`() = runTest {
        val scrollX = 0.1
        val scrollY = 0.2
        val zoom = 5.0
        repository.saveMapState(scrollX, scrollY, zoom)

        val savedState = repository.getSavedMapState()

        assertEquals(scrollX, savedState.scrollX)
        assertEquals(scrollY, savedState.scrollY)
        assertEquals(zoom, savedState.zoom)
    }

    @Test
    fun `getSavedMapState should return default state when nothing is saved`() = runTest {
        val savedState = repository.getSavedMapState()

        assertEquals(MapStateRepositoryImpl.DEFAULT_STATE, savedState)
    }

    @Test
    fun `getSavedMapState should return saved state`() = runTest {
        // Given
        val scrollX = 0.3
        val scrollY = 0.4
        val zoom = 6.0
        repository.saveMapState(scrollX, scrollY, zoom)

        // When
        val result = repository.getSavedMapState()

        // Then
        assertEquals(MapState(scrollX = scrollX, scrollY = scrollY, zoom = zoom), result)
    }
}
