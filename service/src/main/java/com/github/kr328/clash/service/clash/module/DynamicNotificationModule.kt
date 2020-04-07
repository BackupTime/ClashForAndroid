package com.github.kr328.clash.service.clash.module

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.github.kr328.clash.common.ids.Intents
import com.github.kr328.clash.common.ids.NotificationChannels
import com.github.kr328.clash.common.ids.NotificationIds
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.utils.asBytesString
import com.github.kr328.clash.service.R
import com.github.kr328.clash.service.data.ClashDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DynamicNotificationModule(private val service: Service) : Module() {
    override val receiveBroadcasts: Set<String>
        get() = setOf(
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_SCREEN_OFF,
            Intents.INTENT_ACTION_PROFILE_LOADED
        )
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
    private var currentProfile = "Not selected"

    override suspend fun onBroadcastReceived(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON ->
                enableTicker = true
            Intent.ACTION_SCREEN_OFF ->
                enableTicker = false
            Intents.INTENT_ACTION_PROFILE_LOADED ->
                reload()
        }
    }

    override suspend fun onStart() {
        enableTicker = service.getSystemService(PowerManager::class.java).isInteractive
    }

    override suspend fun onStop() {
        service.stopForeground(true)
    }

    override suspend fun onTick() {
        val traffic = Clash.queryTraffic()
        val bandwidth = Clash.queryBandwidth()

        val uploading = traffic.upload.asBytesString()
        val downloading = traffic.download.asBytesString()
        val uploaded = bandwidth.upload.asBytesString()
        val downloaded = bandwidth.download.asBytesString()

        withContext(Dispatchers.Default) {
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

    private suspend fun reload() {
        val active =
            ClashDatabase.getInstance(service).openClashProfileDao().queryActiveProfile()
        currentProfile = active?.name ?: "Not selected"
    }
}