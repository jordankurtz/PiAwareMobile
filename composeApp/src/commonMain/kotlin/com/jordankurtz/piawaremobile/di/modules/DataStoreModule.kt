package com.jordankurtz.piawaremobile.di.modules

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
expect class DataStoreModule() {

    @Single
    fun provideDataStore(contextWrapper: ContextWrapper): DataStore<Preferences>
}
