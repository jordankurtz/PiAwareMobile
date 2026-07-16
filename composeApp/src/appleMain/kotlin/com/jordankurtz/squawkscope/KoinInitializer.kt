package com.jordankurtz.squawkscope

import com.jordankurtz.consolelogger.ConsoleLogger
import com.jordankurtz.logger.Logger
import com.jordankurtz.sentrylogger.SentryLogger
import com.jordankurtz.squawkscope.di.AppKoinApplication
import org.koin.plugin.module.dsl.startKoin

fun startKoin() {
    Logger.addWriter(ConsoleLogger())
    Logger.addWriter(SentryLogger(BuildConfig.SENTRY_DSN))

    startKoin<AppKoinApplication>()
}
