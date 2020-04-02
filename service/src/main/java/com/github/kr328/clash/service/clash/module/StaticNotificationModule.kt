package com.github.kr328.clash.service.clash.module

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.kr328.clash.component.ids.Intents
import com.github.kr328.clash.component.ids.NotificationChannels
import com.github.kr328.clash.component.ids.NotificationIds
import com.github.kr328.clash.service.R
import com.github.kr328.clash.service.data.ClashDatabase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StaticNotificationModule(private val service: Service) : Module {
    private val contentIntent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_DEFAULT)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setPackage(service.packageName)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    private val builder = NotificationCompat.Builder(service, NotificationChannels.CLASH_STATUS)
        .setSmallIcon(R.drawable.ic_notification)
        .setOngoing(true)
        .setColor(service.getColor(R.color.colorAccentService))
        .setOnlyAlertOnce(true)
        .setShowWhen(false)
        .setGroup(NotificationChannels.CLASH_STATUS)
        .setContentIntent(
            PendingIntent.getActivity(
                service,
                NotificationIds.CLASH_STATUS,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
    private val reloadChannel = Channel<Unit>(Channel.CONFLATED)
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intents.INTENT_ACTION_PROFILE_CHANGED ->
                    reloadChannel.offer(Unit)
            }
        }
    }
    private var currentProfile: String = "Not selected"
    private lateinit var backgroundJob: Job

    override suspend fun onCreate() {
        val database = ClashDatabase.getInstance(service).openClashProfileDao()

        backgroundJob = launch {
            while (isActive) {
                reloadChannel.receive()

                currentProfile = database.queryActiveProfile()?.name ?: "Not selected"

                update()
            }
        }
    }

    override suspend fun onStart() {
        service.registerReceiver(receiver, IntentFilter(Intents.INTENT_ACTION_PROFILE_CHANGED))

        reloadChannel.offer(Unit)
    }

    override suspend fun onStop() {
        service.unregisterReceiver(receiver)
        service.stopForeground(true)
    }

    override suspend fun onDestroy() {
        backgroundJob.cancel()
    }

    private fun update() {
        val notification = builder
            .setContentTitle(currentProfile)
            .setContentText(service.getText(R.string.running))
            .build()

        service.startForeground(NotificationIds.CLASH_STATUS, notification)
    }

    companion object {
        fun createNotificationChannel(service: Service) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                return
            NotificationManagerCompat.from(service).createNotificationChannel(
                NotificationChannel(
                    NotificationChannels.CLASH_STATUS,
                    service.getText(R.string.clash_service_status_channel),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        fun notifyLoadingNotification(service: Service) {
            val notification =
                NotificationCompat.Builder(service, NotificationChannels.CLASH_STATUS)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setOngoing(true)
                    .setColor(service.getColor(R.color.colorAccentService))
                    .setOnlyAlertOnce(true)
                    .setShowWhen(false)
                    .setGroup(NotificationChannels.CLASH_STATUS)
                    .setContentTitle(service.getText(R.string.running))
                    .build()

            service.startForeground(NotificationIds.CLASH_STATUS, notification)
        }
    }
}