package com.jordankurtz.squawkscope

import android.app.Application
import com.jordankurtz.consolelogger.ConsoleLogger
import com.jordankurtz.logger.Logger
import com.jordankurtz.sentrylogger.SentryLogger
import com.jordankurtz.squawkscope.di.AppKoinApplication
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.plugin.module.dsl.startKoin

class Application : Application() {
    override fun onCreate() {
        super.onCreate()

        Logger.addWriter(ConsoleLogger())
        Logger.addWriter(SentryLogger(BuildConfig.SENTRY_DSN))

        startKoin<AppKoinApplication> {
            androidContext(this@Application)
            androidLogger()
        }
    }
}
