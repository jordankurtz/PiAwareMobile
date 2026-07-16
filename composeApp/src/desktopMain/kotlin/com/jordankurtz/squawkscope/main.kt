package com.jordankurtz.squawkscope

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.jordankurtz.consolelogger.ConsoleLogger
import com.jordankurtz.logger.Logger
import com.jordankurtz.sentrylogger.SentryLogger
import com.jordankurtz.squawkscope.di.AppKoinApplication
import org.koin.plugin.module.dsl.startKoin

fun main() {
    Logger.addWriter(ConsoleLogger())
    Logger.addWriter(SentryLogger(BuildConfig.SENTRY_DSN))

    startKoin<AppKoinApplication>()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "SquawkScope",
        ) {
            App()
        }
    }
}
