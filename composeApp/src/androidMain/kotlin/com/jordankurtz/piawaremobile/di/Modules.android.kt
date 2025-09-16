package com.jordankurtz.piawaremobile.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val dataStoreModule: Module
    get() = module { single { PreferenceDataStoreFactory.createWithPath { androidContext().filesDir.resolve("settings.preferences_pb").absolutePath.toPath() } } }