package com.jordankurtz.piawaremobile

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.jordankurtz.consolelogger.ConsoleLogger
import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.di.modules.AppModule
import com.jordankurtz.sentrylogger.SentryLogger
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

fun main() {
    Logger.addWriter(ConsoleLogger())
    Logger.addWriter(SentryLogger(BuildConfig.SENTRY_DSN))

    startKoin {
        modules(AppModule().module)
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "PiAware Mobile",
        ) {
            App()
        }
    }
}
