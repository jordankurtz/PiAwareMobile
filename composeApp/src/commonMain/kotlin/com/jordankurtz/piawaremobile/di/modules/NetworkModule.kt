package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.KtorClient
import io.ktor.client.HttpClient
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
object NetworkModule {
    @Single
    fun provideHttpClient(): HttpClient {
        return KtorClient().client
    }
}
