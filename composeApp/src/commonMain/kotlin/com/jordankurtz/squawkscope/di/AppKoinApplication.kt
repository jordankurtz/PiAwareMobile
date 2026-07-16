package com.jordankurtz.squawkscope.di

import com.jordankurtz.squawkscope.di.modules.AppModule
import org.koin.core.annotation.KoinApplication

@KoinApplication(modules = [AppModule::class])
class AppKoinApplication
