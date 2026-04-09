package com.jordankurtz.piawaremobile.map.offline

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CancelDownloadReceiver : BroadcastReceiver(), KoinComponent {
    private val coordinator: BackgroundDownloadCoordinator by inject()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        coordinator.cancel()
    }
}
