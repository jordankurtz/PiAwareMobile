package com.jordankurtz.piawaremobile

import androidx.compose.ui.window.ComposeUIViewController
import com.jordankurtz.piawaremobile.di.initKoin

fun MainViewController() = ComposeUIViewController(configure = { initKoin() }) { App() }