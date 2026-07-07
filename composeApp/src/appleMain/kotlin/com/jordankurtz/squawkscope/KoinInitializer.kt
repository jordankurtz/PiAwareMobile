package com.jordankurtz.squawkscope

import com.jordankurtz.consolelogger.ConsoleLogger
import com.jordankurtz.logger.Logger
import com.jordankurtz.sentrylogger.SentryLogger
import com.jordankurtz.squawkscope.di.modules.AppModule
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

fun startKoin() {
    Logger.addWriter(ConsoleLogger())
    Logger.addWriter(SentryLogger(BuildConfig.SENTRY_DSN))

    startKoin {
        modules(AppModule().module)
    }
}
