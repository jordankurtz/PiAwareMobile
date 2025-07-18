package com.jordankurtz.piawareviewer

import androidx.compose.ui.window.ComposeUIViewController
import com.jordankurtz.piawareviewer.di.initKoin

fun MainViewController() = ComposeUIViewController(configure = { initKoin() }) { App() }