package com.github.kr328.clash.service.clash.module

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.github.kr328.clash.component.ids.Intents
import com.github.kr328.clash.component.ids.NotificationChannels
import com.github.kr328.clash.component.ids.NotificationIds
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.utils.asBytesString
import com.github.kr328.clash.service.R
import com.github.kr328.clash.service.data.ClashDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select

class DynamicNotificationModule(private val service: Service) : NotificationModule {
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
    private val screenChannel = Channel<Boolean>(Channel.CONFLATED)
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON ->
                    screenChannel.offer(true)
                Intent.ACTION_SCREEN_OFF ->
                    screenChannel.offer(false)
                Intents.INTENT_ACTION_PROFILE_CHANGED ->
                    reloadChannel.offer(Unit)
            }
        }
    }
    private lateinit var backgroundJob: Job
    private var currentProfile = "Not selected"

    override suspend fun onCreate() {
        withContext(Dispatchers.Unconfined) {
            val database = ClashDatabase.getInstance(service).openClashProfileDao()
            val tickerChannel = Channel<Unit>()

            backgroundJob = launch {
                launch {
                    while (isActive) {
                        tickerChannel.send(Unit)
                        delay(1000)
                    }
                }

                var refreshEnabled =
                    service.getSystemService(PowerManager::class.java).isInteractive

                while (isActive) {
                    select<Unit> {
                        reloadChannel.onReceive {
                            currentProfile = database.queryActiveProfile()?.name ?: "Not selected"
                        }
                        screenChannel.onReceive {
                            refreshEnabled = it
                        }
                        if (refreshEnabled) {
                            tickerChannel.onReceive {
                                update()
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun onStart() {
        reloadChannel.offer(Unit)

        service.registerReceiver(receiver, IntentFilter().apply {
            addAction(Intents.INTENT_ACTION_PROFILE_CHANGED)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        })
    }

    override suspend fun onStop() {
        service.unregisterReceiver(receiver)
    }

    override suspend fun onDestroy() {
        backgroundJob.cancel()
    }

    override fun update() {
        val traffic = Clash.queryTraffic()
        val bandwidth = Clash.queryBandwidth()

        val uploading = traffic.upload.asBytesString()
        val downloading = traffic.download.asBytesString()
        val uploaded = bandwidth.upload.asBytesString()
        val downloaded = bandwidth.download.asBytesString()

        val notification = builder
            .setContentTitle(currentProfile)
            .setContentText(
                service.getString(
                    R.string.clash_notification_content,
                    uploading, downloading
                )
            )
            .setSubText(
                service.getString(
                    R.string.clash_notification_content,
                    uploaded, downloaded
                )
            )
            .build()

        service.startForeground(NotificationIds.CLASH_STATUS, notification)
    }
}