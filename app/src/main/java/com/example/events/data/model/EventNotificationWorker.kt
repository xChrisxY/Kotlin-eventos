package com.example.events.data.model

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.events.R

class EventNotificationWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    companion object {
        const val EVENT_NAME = "event_name"
        const val EVENT_DESCRIPTION = "event_description"
        const val CHANNEL_ID = "event_reminder_channel"
        const val NOTIFICATION_ID = 100
    }

    override fun doWork(): Result {
        val eventName = inputData.getString(EVENT_NAME)
        val eventDescription = inputData.getString(EVENT_DESCRIPTION)

        if (eventName != null && eventDescription != null) {
            showNotification(eventName, eventDescription)
        } else {
            Log.e("EventNotificationWorker", "Missing event details.")
        }

        return Result.success()
    }

    private fun showNotification(eventName: String, eventDescription: String) {
        createNotificationChannel()

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Reemplaza con tu icono
            .setContentTitle("¡Evento Pronto: $eventName!")
            .setContentText(eventDescription)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(applicationContext)) {
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("A", "No se concedió el permiso")
                return
            } else{
                Log.d("A", "Si se concedió el permiso")
            }
            notify(NOTIFICATION_ID, builder.build())

        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Event Reminder Channel"
            val descriptionText = "Channel for event reminder notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system.
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}