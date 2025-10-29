package com.jordankurtz.piawaremobile.di

import com.jordankurtz.piawaremobile.KtorClient
import com.jordankurtz.piawaremobile.aircraft.api.PiAwareApi
import com.jordankurtz.piawaremobile.aircraft.api.PiAwareApiImpl
import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepoImpl
import com.jordankurtz.piawaremobile.aircraft.usecase.GetAircraftWithDetailsUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.GetReceiverLocationUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.LoadAircraftTypesUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.impl.GetAircraftWithDetailsUseCaseImpl
import com.jordankurtz.piawaremobile.aircraft.usecase.impl.GetReceiverLocationUseCaseImpl
import com.jordankurtz.piawaremobile.aircraft.usecase.impl.LoadAircraftTypesUseCaseImpl
import com.jordankurtz.piawaremobile.map.MapViewModel
import com.jordankurtz.piawaremobile.map.OpenStreetMapProvider
import com.jordankurtz.piawaremobile.map.repo.MapStateRepository
import com.jordankurtz.piawaremobile.map.repo.MapStateRepositoryImpl
import com.jordankurtz.piawaremobile.map.usecase.GetSavedMapStateUseCase
import com.jordankurtz.piawaremobile.map.usecase.SaveMapStateUseCase
import com.jordankurtz.piawaremobile.map.usecase.impl.GetSavedMapStateUseCaseImpl
import com.jordankurtz.piawaremobile.map.usecase.impl.SaveMapStateUseCaseImpl
import com.jordankurtz.piawaremobile.settings.SettingsViewModel
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepositoryImpl
import com.jordankurtz.piawaremobile.settings.usecase.AddServerUseCase
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetCenterMapOnUserOnStartUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetOpenUrlsExternallyUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetRefreshIntervalUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetRestoreMapStateOnStartUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetShowReceiverLocationsUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetShowUserLocationOnMapUseCase
import com.jordankurtz.piawaremobile.settings.usecase.impl.AddServerUseCaseImpl
import com.jordankurtz.piawaremobile.settings.usecase.impl.LoadSettingsUseCaseImpl
import com.jordankurtz.piawaremobile.settings.usecase.impl.SetCenterMapOnUserOnStartUseCaseImpl
import com.jordankurtz.piawaremobile.settings.usecase.impl.SetOpenUrlsExternallyUseCaseImpl
import com.jordankurtz.piawaremobile.settings.usecase.impl.SetRefreshIntervalUseCaseImpl
import com.jordankurtz.piawaremobile.settings.usecase.impl.SetRestoreMapStateOnStartUseCaseImpl
import com.jordankurtz.piawaremobile.settings.usecase.impl.SetShowReceiverLocationsUseCaseImpl
import com.jordankurtz.piawaremobile.settings.usecase.impl.SetShowUserLocationOnMapUseCaseImpl
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ovh.plrapps.mapcompose.core.TileStreamProvider

val viewModelModule = module {
    viewModel {
        MapViewModel(
            mapProvider = get(),
            loadSettingsUseCase = get(),
            urlHandler = get(),
            locationService = get(),
            getSavedMapStateUseCase = get(),
            saveMapStateUseCase = get(),
            loadAircraftTypesUseCase = get(),
            getAircraftWithDetailsUseCase = get(),
            getReceiverLocationUseCase = get(),
            ioDispatcher = get(named("IODispatcher")),
            mainDispatcher = get(named("MainDispatcher"))
        )
    }
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
    single<PiAwareApi> { PiAwareApiImpl(get()) }
}

val useCaseModule = module {
    // Settings
    singleOf(::LoadSettingsUseCaseImpl) { bind<LoadSettingsUseCase>() }
    singleOf(::AddServerUseCaseImpl) { bind<AddServerUseCase>() }
    singleOf(::SetRefreshIntervalUseCaseImpl) { bind<SetRefreshIntervalUseCase>() }
    singleOf(::SetCenterMapOnUserOnStartUseCaseImpl) { bind<SetCenterMapOnUserOnStartUseCase>() }
    singleOf(::SetRestoreMapStateOnStartUseCaseImpl) { bind<SetRestoreMapStateOnStartUseCase>() }
    singleOf(::SetShowReceiverLocationsUseCaseImpl) { bind<SetShowReceiverLocationsUseCase>() }
    singleOf(::SetShowUserLocationOnMapUseCaseImpl) { bind<SetShowUserLocationOnMapUseCase>() }
    singleOf(::SetOpenUrlsExternallyUseCaseImpl) { bind<SetOpenUrlsExternallyUseCase>() }

    singleOf(::SaveMapStateUseCaseImpl) { bind<SaveMapStateUseCase>() }
    singleOf(::GetSavedMapStateUseCaseImpl) { bind<GetSavedMapStateUseCase>() }

    singleOf(::GetAircraftWithDetailsUseCaseImpl) { bind<GetAircraftWithDetailsUseCase>() }
    singleOf(::LoadAircraftTypesUseCaseImpl) { bind<LoadAircraftTypesUseCase>() }
    singleOf(::GetReceiverLocationUseCaseImpl) { bind<GetReceiverLocationUseCase>() }
}

val dispatcherModule = module {
    single<CoroutineDispatcher>(named("IODispatcher")) { Dispatchers.IO }
    single<CoroutineDispatcher>(named("MainDispatcher")) { Dispatchers.Main }
}

expect val platformModule: Module
