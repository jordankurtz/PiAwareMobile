package com.jordankurtz.piawareviewer.di

import com.jordankurtz.piawareviewer.KtorClient
import com.jordankurtz.piawareviewer.api.PiAwareApi
import com.jordankurtz.piawareviewer.map.MapViewModel
import com.jordankurtz.piawareviewer.map.OpenStreetMapProvider
import com.jordankurtz.piawareviewer.settings.SettingsViewModel
import com.jordankurtz.piawareviewer.settings.repo.SettingsRepository
import com.jordankurtz.piawareviewer.settings.repo.SettingsRepositoryImpl
import com.jordankurtz.piawareviewer.settings.usecase.AddServerUseCase
import com.jordankurtz.piawareviewer.settings.usecase.LoadSettingsUseCase
import com.jordankurtz.piawareviewer.settings.usecase.SetRefreshIntervalUseCase
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

