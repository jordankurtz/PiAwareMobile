package com.jordankurtz.piawaremobile.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.jordankurtz.piawaremobile.UrlHandler
import com.jordankurtz.piawaremobile.location.LocationService
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module
    get() = module {
        single {
            PreferenceDataStoreFactory.createWithPath {
                ("/settings").toPath()
            }
        }

        single { UrlHandler() }

        single {
            LocationService()
        }
    }