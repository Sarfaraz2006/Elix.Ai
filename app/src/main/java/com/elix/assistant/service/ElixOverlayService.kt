package com.elix.assistant.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.elix.assistant.R

class ElixOverlayService : Service() {
    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification() =
        NotificationCompat.Builder(this, ensureChannel())
            .setSmallIcon(R.drawable.ic_elix_notif)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Elix overlay running")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

    private fun ensureChannel(): String {
        val channelId = "elix_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Elix Overlay", NotificationManager.IMPORTANCE_LOW),
            )
        }
        return channelId
    }
}

