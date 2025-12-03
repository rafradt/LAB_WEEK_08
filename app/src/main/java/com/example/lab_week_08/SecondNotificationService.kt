package com.example.lab_week_08

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData

class SecondNotificationService : Service() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Corrected line: Call .build() to get the Notification object
        startForeground(200, buildNotification("Starting...").build())

        handler.post {
            countdown()
            notifyCompletion()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun countdown() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val builder = buildNotification("")

        for (i in 5 downTo 0) {
            Thread.sleep(700L)
            builder.setContentText("$i seconds remaining")

            manager.notify(200, builder.build())
        }
    }

    private fun notifyCompletion() {
        completionLiveData.postValue(true)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "SECOND_SERVICE",
                "Second Foreground Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, "SECOND_SERVICE")
            .setContentTitle("Second Notification Service")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setSilent(true)
    }

    companion object {
        val completionLiveData = MutableLiveData<Boolean>()
    }
}
