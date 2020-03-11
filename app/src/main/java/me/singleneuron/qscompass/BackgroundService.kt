package me.singleneuron.qscompass

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class BackgroundService : Service() {

     private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun stopBackgroundService() {
        stopSelf()
    }

    inner class LocalBinder : Binder() {
        fun getService() : BackgroundService = this@BackgroundService
    }

}
