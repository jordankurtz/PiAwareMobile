package com.jordankurtz.piawaremobile

import androidx.compose.ui.window.ComposeUIViewController
import com.jordankurtz.piawaremobile.di.modules.AppModule
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

fun MainViewController() = ComposeUIViewController(configure = { startKoin {
    modules(AppModule().module)
} }) { App() }
