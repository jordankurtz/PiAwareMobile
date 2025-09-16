package com.jordankurtz.piawaremobile.di

import com.jordankurtz.piawaremobile.KtorClient
import com.jordankurtz.piawaremobile.api.PiAwareApi
import com.jordankurtz.piawaremobile.map.MapViewModel
import com.jordankurtz.piawaremobile.map.OpenStreetMapProvider
import com.jordankurtz.piawaremobile.settings.SettingsViewModel
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepositoryImpl
import com.jordankurtz.piawaremobile.settings.usecase.AddServerUseCase
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetRefreshIntervalUseCase
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ovh.plrapps.mapcompose.core.TileStreamProvider

val viewModelModule = module {
    viewModelOf(::MapViewModel)
    viewModelOf(::SettingsViewModel)
}

val dataModule = module {
    singleOf(::SettingsRepositoryImpl) { bind<SettingsRepository>() }
}

val networkModule = module {
    single<HttpClient> { KtorClient().client }
    singleOf(::OpenStreetMapProvider) { bind<TileStreamProvider>() }
}

val apiModule = module {
    single { PiAwareApi(get()) }
}

val useCaseModule = module {
    single{ LoadSettingsUseCase(get()) }
    single{ AddServerUseCase(get()) }
    single{ SetRefreshIntervalUseCase(get()) }
}

expect val dataStoreModule: Module

