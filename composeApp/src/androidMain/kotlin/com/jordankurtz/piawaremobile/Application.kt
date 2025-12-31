package com.jordankurtz.piawaremobile

import android.app.Application
import com.jordankurtz.consolelogger.ConsoleLogger
import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.di.modules.AppModule
import com.jordankurtz.sentrylogger.SentryLogger
import io.sentry.kotlin.multiplatform.Sentry
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

class Application: Application() {

    override fun onCreate() {
        super.onCreate()

        Logger.addWriter(ConsoleLogger())
        Logger.addWriter(SentryLogger(BuildConfig.SENTRY_DSN))

        startKoin {
            androidContext(this@Application)
            androidLogger()
            modules(AppModule().module)
        }
    }
}
