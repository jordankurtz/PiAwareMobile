package com.jordankurtz.piawareviewer.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual val dataStoreModule: Module
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
    }