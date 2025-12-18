package com.jordankurtz.piawaremobile

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.jordankurtz.consolelogger.ConsoleLogger
import com.jordankurtz.logger.Logger
import com.jordankurtz.sentrylogger.SentryLogger
import io.sentry.kotlin.multiplatform.Sentry

fun main() = application {
    Logger.addWriter(ConsoleLogger())
    Logger.addWriter(SentryLogger(BuildConfig.SENTRY_DSN))

    Window(
        onCloseRequest = ::exitApplication,
        title = "PiAware Mobile",
    ) {
        App()
    }
}
