package com.jordankurtz.piawaremobile.map.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FakeDataStore(initialPreferences: Preferences = emptyPreferences()) : DataStore<Preferences> {

    private val flow = MutableStateFlow(initialPreferences)
    private val mutex = Mutex()

    override val data: Flow<Preferences>
        get() = flow

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        mutex.withLock {
            val updated = transform(flow.value)
            flow.value = updated
            return updated
        }
    }
}
