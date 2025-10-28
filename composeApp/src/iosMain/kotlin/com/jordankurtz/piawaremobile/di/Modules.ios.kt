package com.jordankurtz.piawaremobile.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.jordankurtz.piawaremobile.UrlHandler
import com.jordankurtz.piawaremobile.UrlHandlerImpl
import com.jordankurtz.piawaremobile.location.LocationService
import com.jordankurtz.piawaremobile.location.LocationServiceImpl
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual val platformModule: Module
    get() = module {
        single {
            PreferenceDataStoreFactory.createWithPath {
                (requireNotNull(
                    NSSearchPathForDirectoriesInDomains(
                        NSApplicationSupportDirectory,
                        NSUserDomainMask,
                        true
                    ).firstOrNull()?.toString()
                ) + "/settings.preferences_pb").toPath()
            }
        }

        singleOf(::UrlHandlerImpl) { bind<UrlHandler>() }
        singleOf(::LocationServiceImpl) { bind<LocationService>() }
    }
