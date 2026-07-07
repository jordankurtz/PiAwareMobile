package com.jordankurtz.squawkscope.di.modules

import com.jordankurtz.squawkscope.KtorClient
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
