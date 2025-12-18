package com.jordankurtz.piawaremobile

import androidx.compose.ui.window.ComposeUIViewController
import com.jordankurtz.consolelogger.ConsoleLogger
import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.di.modules.AppModule
import com.jordankurtz.sentrylogger.SentryLogger
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

fun MainViewController() = ComposeUIViewController(configure = {
    Logger.addWriter(ConsoleLogger())
    Logger.addWriter(SentryLogger(BuildConfig.SENTRY_DSN))

    startKoin {
        modules(AppModule().module)
    }
}) { App() }
