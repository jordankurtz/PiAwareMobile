package com.jordankurtz.piawaremobile.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.jordankurtz.piawaremobile.UrlHandler
import com.jordankurtz.piawaremobile.UrlHandlerImpl
import com.jordankurtz.piawaremobile.location.LocationService
import com.jordankurtz.piawaremobile.location.LocationServiceImpl
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual val platformModule: Module
    get() = module {
        single {
            PreferenceDataStoreFactory.createWithPath {
                androidContext().filesDir.resolve(
                    "settings.preferences_pb"
                ).absolutePath.toPath()
            }
        }
        singleOf(::UrlHandlerImpl) { bind<UrlHandler>() }

        singleOf(::LocationServiceImpl) { bind<LocationService>() }
    }
