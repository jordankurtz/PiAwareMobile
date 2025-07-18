package com.jordankurtz.piawareviewer.di

import com.jordankurtz.piawareviewer.KtorClient
import com.jordankurtz.piawareviewer.api.PiAwareApi
import com.jordankurtz.piawareviewer.map.MapViewModel
import com.jordankurtz.piawareviewer.map.OpenStreetMapProvider
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ovh.plrapps.mapcompose.core.TileStreamProvider

val viewModelModule = module {
    viewModelOf(::MapViewModel)
}

val networkModule = module {
    single<HttpClient> { KtorClient().client }
    singleOf(::OpenStreetMapProvider) { bind<TileStreamProvider>() }
}

val apiModule = module {
    single { PiAwareApi(get()) }
}
