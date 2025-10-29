package com.jordankurtz.piawaremobile.di.modules

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DispatchersModule::class,
    NetworkModule::class,
    ContextModule::class,
    DataStoreModule::class])
@ComponentScan("com.jordankurtz.piawaremobile")
class AppModule
