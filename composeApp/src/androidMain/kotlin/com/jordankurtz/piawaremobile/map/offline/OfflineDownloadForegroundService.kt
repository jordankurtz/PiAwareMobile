package com.jordankurtz.piawaremobile.map.offline

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jordankurtz.piawaremobile.MainActivity
import com.jordankurtz.piawaremobile.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class OfflineDownloadForegroundService : Service() {
    private val coordinator: BackgroundDownloadCoordinator by inject()
    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START -> {
                val name = intent.getStringExtra(EXTRA_REGION_NAME) ?: "Map region"
                startForeground(NOTIFICATION_ID, buildProgressNotification(name, 0L, 0L))
                progressJob =
                    serviceScope.launch {
                        coordinator.progress.collect { progress ->
                            if (progress != null) {
                                notificationManager.notify(
                                    NOTIFICATION_ID,
                                    buildProgressNotification(name, progress.downloaded, progress.total),
                                )
                            }
                        }
                    }
            }
            ACTION_COMPLETE -> {
                val name = intent.getStringExtra(EXTRA_REGION_NAME) ?: "Map region"
                stopProgress()
                notificationManager.notify(COMPLETE_NOTIFICATION_ID, buildCompleteNotification(name))
                stopSelf()
            }
            ACTION_FAILED -> {
                val name = intent.getStringExtra(EXTRA_REGION_NAME) ?: "Map region"
                stopProgress()
                notificationManager.notify(COMPLETE_NOTIFICATION_ID, buildFailedNotification(name))
                stopSelf()
            }
            ACTION_CANCELLED -> {
                stopProgress()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun stopProgress() {
        progressJob?.cancel()
        progressJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Offline Map Downloads",
                    NotificationManager.IMPORTANCE_LOW,
                )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildProgressNotification(
        name: String,
        downloaded: Long,
        total: Long,
    ) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_plane)
        .setContentTitle("Downloading $name")
        .setContentText(if (total > 0L) "$downloaded / $total tiles" else "Starting download\u2026")
        .setProgress(
            total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            downloaded.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            total == 0L,
        )
        .setOngoing(true)
        .addAction(
            0,
            "Cancel",
            PendingIntent.getBroadcast(
                this,
                0,
                Intent(this, CancelDownloadReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .build()

    private fun buildCompleteNotification(name: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_plane)
            .setContentTitle("$name downloaded")
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()

    private fun buildFailedNotification(name: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_plane)
            .setContentTitle("$name download failed")
            .setContentText("Tap to retry")
            .setContentIntent(openAppIntent())
            .build()

    private fun openAppIntent() =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        const val ACTION_START = "com.jordankurtz.piawaremobile.action.DOWNLOAD_START"
        const val ACTION_COMPLETE = "com.jordankurtz.piawaremobile.action.DOWNLOAD_COMPLETE"
        const val ACTION_FAILED = "com.jordankurtz.piawaremobile.action.DOWNLOAD_FAILED"
        const val ACTION_CANCELLED = "com.jordankurtz.piawaremobile.action.DOWNLOAD_CANCELLED"
        const val EXTRA_REGION_NAME = "region_name"
        const val CHANNEL_ID = "offline_map_downloads"
        const val NOTIFICATION_ID = 1001
        const val COMPLETE_NOTIFICATION_ID = 1002
    }
}
