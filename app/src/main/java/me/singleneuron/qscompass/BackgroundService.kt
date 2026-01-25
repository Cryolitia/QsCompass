package me.singleneuron.qscompass

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat

class BackgroundService : Service() {

     private val binder = LocalBinder()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification : Notification =
            NotificationCompat.Builder(this,"channelID")
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.running))
                .setTicker(getText(R.string.running))
                .setSmallIcon(R.drawable.navigation)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVibrate(LongArray(0))
                .setSound(null)
                .build()
        startForeground(1,notification)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun stopBackgroundService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun stopListening() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    inner class LocalBinder : Binder() {
        fun getService() : BackgroundService = this@BackgroundService
    }

}
