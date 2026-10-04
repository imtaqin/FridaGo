package com.imtaqin.corvo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.imtaqin.corvo.MainActivity
import com.imtaqin.corvo.R
import com.imtaqin.corvo.core.FridaController

/**
 * Holds a persistent notification while frida-server is meant to be running, so
 * Android doesn't reap the app and the user has a quick way back into Corvo.
 * The server itself is detached via setsid, so this service is purely a keep-alive
 * + status surface.
 */
class FridaForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        createChannel()
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("frida-server active on ${FridaController.endpoint}")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Frida server",
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        }
    }

    companion object {
        private const val CHANNEL_ID = "frida_server"
        private const val NOTIF_ID = 42
        const val ACTION_STOP = "com.imtaqin.corvo.STOP_SERVICE"

        fun start(ctx: Context) {
            val i = Intent(ctx, FridaForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }

        fun stop(ctx: Context) {
            ctx.startService(
                Intent(ctx, FridaForegroundService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
