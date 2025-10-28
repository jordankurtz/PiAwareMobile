package com.jordankurtz.piawaremobile.settings.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.testutil.FakeDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepositoryImplTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepository

    @BeforeTest
    fun setUp() {
        dataStore = FakeDataStore()
        repository = SettingsRepositoryImpl(dataStore)
    }

    @Test
    fun `getSettings should return default settings when nothing is saved`() = runTest {
        val settings = repository.getSettings().first()

        assertEquals(Settings(
            servers = emptyList(),
            refreshInterval = 5,
            centerMapOnUserOnStart = false,
            restoreMapStateOnStart = false,
            showReceiverLocations = false,
            showUserLocationOnMap = false
        ), settings)
    }

    @Test
    fun `saveSettings should store the settings`() = runTest {
        val newSettings = Settings(
            servers = listOf(),
            refreshInterval = 30,
            centerMapOnUserOnStart = true,
            restoreMapStateOnStart = true,
            showReceiverLocations = true,
            showUserLocationOnMap = true
        )

        repository.saveSettings(newSettings)

        val savedSettings = repository.getSettings().first()

        assertEquals(newSettings, savedSettings)
    }
}
