package com.jordankurtz.piawaremobile

import android.app.Application
import com.jordankurtz.piawaremobile.di.modules.AppModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

class Application: Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@Application)
            androidLogger()
            modules(AppModule().module)
        }
    }
}
