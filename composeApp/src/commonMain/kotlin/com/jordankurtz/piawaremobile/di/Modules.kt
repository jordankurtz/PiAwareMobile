package com.jordankurtz.piawaremobile.di

import com.jordankurtz.piawaremobile.KtorClient
import com.jordankurtz.piawaremobile.aircraft.api.PiAwareApi
import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepoImpl
import com.jordankurtz.piawaremobile.aircraft.usecase.GetAircraftWithDetailsUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.LoadAircraftTypesUseCase
import com.jordankurtz.piawaremobile.map.MapViewModel
import com.jordankurtz.piawaremobile.map.OpenStreetMapProvider
import com.jordankurtz.piawaremobile.map.repo.MapStateRepository
import com.jordankurtz.piawaremobile.map.repo.MapStateRepositoryImpl
import com.jordankurtz.piawaremobile.map.usecase.GetSavedMapStateUseCase
import com.jordankurtz.piawaremobile.map.usecase.SaveMapStateUseCase
import com.jordankurtz.piawaremobile.settings.SettingsViewModel
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepositoryImpl
import com.jordankurtz.piawaremobile.settings.usecase.AddServerUseCase
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetCenterMapOnUserOnStartUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetRefreshIntervalUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetRestoreMapStateOnStartUseCase
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
    singleOf(::MapStateRepositoryImpl) { bind<MapStateRepository>() }
    singleOf(::AircraftRepoImpl) { bind<AircraftRepo>() }
}

val networkModule = module {
    single<HttpClient> { KtorClient().client }
    singleOf(::OpenStreetMapProvider) { bind<TileStreamProvider>() }
}

val apiModule = module {
    single { PiAwareApi(get()) }
}

val useCaseModule = module {
    single { LoadSettingsUseCase(get()) }
    single { AddServerUseCase(get()) }
    single { SetRefreshIntervalUseCase(get()) }
    single { SetCenterMapOnUserOnStartUseCase(get()) }
    single { SetRestoreMapStateOnStartUseCase(get()) }

    single { SaveMapStateUseCase(get()) }
    single { GetSavedMapStateUseCase(get()) }

    single { GetAircraftWithDetailsUseCase(get()) }
    single { LoadAircraftTypesUseCase(get()) }
}

expect val platformModule: Module
