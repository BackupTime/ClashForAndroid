package com.github.kr328.clash.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.utils.asBytesString
import com.github.kr328.clash.core.utils.asSpeedString
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class ClashNotification(private val context: ClashService, enableRefresh: Boolean) :
    CoroutineScope by context {
    companion object {
        private const val CLASH_STATUS_NOTIFICATION_CHANNEL = "clash_status_channel"
        private const val CLASH_STATUS_NOTIFICATION_ID = 413
    }

    private val contentIntent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_DEFAULT)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setPackage(context.packageName)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    private val baseBuilder = NotificationCompat.Builder(context, CLASH_STATUS_NOTIFICATION_CHANNEL)
        .setSmallIcon(R.drawable.ic_notification)
        .setOngoing(true)
        .setColor(context.getColor(R.color.colorAccentService))
        .setOnlyAlertOnce(true)
        .setShowWhen(false)
        .setContentIntent(
            PendingIntent.getActivity(
                context,
                CLASH_STATUS_NOTIFICATION_ID,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
    private val screenChannel: Channel<Boolean> = Channel(Channel.CONFLATED)
    private val tickerChannel: Channel<Unit> = Channel()

    private var profile = "None"
    private val observer = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON ->
                    screenChannel.offer(true)
                Intent.ACTION_SCREEN_OFF ->
                    screenChannel.offer(false)
            }
        }
    }

    init {
        createNotificationChannel()

        runBlocking {
            update()
        }

        if (enableRefresh) {
            launch {
                val powerManager =
                    requireNotNull(context.getSystemService(PowerManager::class.java))

                screenChannel.send(powerManager.isInteractive)

                var tickerJob: Job? = null

                launch {
                    while (isActive) {
                        tickerJob = if (screenChannel.receive()) {
                            tickerJob?.cancel()
                            startTicker()
                        } else {
                            tickerJob?.cancel()
                            null
                        }
                    }
                }

                launch {
                    while (isActive) {
                        tickerChannel.receive()

                        update()
                    }
                }

                context.registerReceiver(observer, IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_SCREEN_OFF)
                })
            }
        }
    }

    private fun startTicker(): Job {
        return launch {
            while (isActive) {
                tickerChannel.send(Unit)

                delay(1000)
            }
        }
    }

    fun destroy() {
        context.unregisterReceiver(observer)
        context.stopForeground(true)
    }

    fun setProfile(profile: String) {
        launch {
            this@ClashNotification.profile = profile

            update()
        }
    }

    private suspend fun update() {
        val notification = withContext(Dispatchers.Default) {
            createNotification()
        }

        context.startForeground(CLASH_STATUS_NOTIFICATION_ID, notification)
    }

    private fun createNotification(): Notification {
        val traffic = Clash.queryTraffic()
        val bandwidth = Clash.queryBandwidth()

        return baseBuilder
            .setContentTitle(profile)
            .setContentText(
                context.getString(
                    R.string.clash_notification_content,
                    traffic.upload.asSpeedString(),
                    traffic.download.asSpeedString()
                )
            )
            .setSubText(
                context.getString(
                    R.string.clash_notification_content,
                    bandwidth.upload.asBytesString(),
                    bandwidth.download.asBytesString()
                )
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return
        NotificationManagerCompat.from(context).createNotificationChannel(
            NotificationChannel(
                CLASH_STATUS_NOTIFICATION_CHANNEL,
                context.getText(R.string.clash_service_status_channel),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }
}