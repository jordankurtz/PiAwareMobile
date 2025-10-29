package com.jordankurtz.piawaremobile.di.modules

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class DataStoreModule {
    @Single
    actual fun provideDataStore(contextWrapper: ContextWrapper): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath {
            ("settings").toPath()
        }
    }
}
