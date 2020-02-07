package com.github.kr328.clash.service

import android.app.*
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
import com.github.kr328.clash.service.util.ticker
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class ClashNotification(private val context: Service) : CoroutineScope {
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
        runBlocking {
            update()
        }

        launch {
            withContext(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationManagerCompat.from(context)
                        .createNotificationChannel(
                            NotificationChannel(
                                CLASH_STATUS_NOTIFICATION_CHANNEL,
                                context.getString(R.string.clash_service_status_channel),
                                NotificationManager.IMPORTANCE_LOW
                            )
                        )
                }
            }

            while (isActive) {
                val powerManager =
                    requireNotNull(context.getSystemService(PowerManager::class.java))
                val tickerChannel = Channel<Int>(Channel.CONFLATED)
                var tickerJob = if (powerManager.isInteractive)
                    ticker(1000, tickerChannel)
                else
                    EmptyCoroutineContext

                select<Unit> {
                    screenChannel.onReceive {
                        tickerJob.cancel()

                        if (it) {
                            tickerJob = ticker(1000, tickerChannel)
                        }
                    }
                    tickerChannel.onReceive {
                        update()
                    }
                }
            }
        }

        context.registerReceiver(observer, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
    }

    fun destroy() {
        cancel()

        context.unregisterReceiver(observer)
        context.stopForeground(true)
    }

    fun setProfile(profile: String) {
        this.profile = profile
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
                    traffic.download.asBytesString()
                )
            )
            .build()
    }

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob()
}