package com.jordankurtz.piawaremobile.map.offline

import org.koin.mp.KoinPlatform

fun getIosBackgroundDownloadCoordinator(): IosBackgroundDownloadCoordinator = KoinPlatform.getKoin().get()
