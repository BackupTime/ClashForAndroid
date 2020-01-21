package com.github.kr328.clash.service

import android.app.*
import android.content.*
import android.os.Build
import android.os.Handler
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.event.EventStream
import com.github.kr328.clash.core.event.TrafficEvent
import com.github.kr328.clash.core.utils.ByteFormatter

class ClashNotification(private val context: Service) {
    companion object {
        private const val CLASH_STATUS_NOTIFICATION_CHANNEL = "clash_status_channel"
        private const val CLASH_STATUS_NOTIFICATION_ID = 413

        private const val MAIN_ACTIVITY_NAME = ".MainActivity"
    }

    private val handler = Handler()
    private var showing = false

    private val baseBuilder = NotificationCompat.Builder(context, CLASH_STATUS_NOTIFICATION_CHANNEL)
        .setSmallIcon(R.drawable.ic_notification_icon)
        .setOngoing(true)
        .setColor(context.getColor(R.color.colorAccentService))
        .setOnlyAlertOnce(true)
        .setShowWhen(false)
        .setContentIntent(
            PendingIntent.getActivity(
                context,
                (Math.random() * 100).toInt(),
                Intent().setComponent(
                    ComponentName.createRelative(
                        context,
                        MAIN_ACTIVITY_NAME
                    )
                ).setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                ),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        )

    private var vpn = false
    private var up = 0L
    private var down = 0L
    private var profile = "None"
    private var traffic: EventStream<TrafficEvent>? = null
    private val observer = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON ->
                    enableUpdate()
                Intent.ACTION_SCREEN_OFF ->
                    disableUpdate()
            }
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManagerCompat.from(context)
                .createNotificationChannel(
                    NotificationChannel(
                        CLASH_STATUS_NOTIFICATION_CHANNEL,
                        context.getString(R.string.clash_service_status_channel),
                        NotificationManager.IMPORTANCE_MIN
                    )
                )
        }

        handler.post {
            showing = true

            update()
        }

        context.registerReceiver(observer, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })

        if (context.getSystemService(PowerManager::class.java)!!.isInteractive) {
            enableUpdate()
        }
    }

    fun destroy() {
        handler.post {
            disableUpdate()

            if (showing)
                context.stopForeground(true)

            showing = false
        }
    }

    fun setProfile(profile: String) {
        handler.post {
            this.profile = profile

            update()
        }
    }

    fun setVpn(vpn: Boolean) {
        handler.post {
            this.vpn = vpn

            update()
        }
    }

    private fun setSpeed(up: Long, down: Long) {
        handler.post {
            this.up = up
            this.down = down

            update()
        }
    }

    private fun enableUpdate() {
        handler.post {
            if (traffic != null)
                return@post

            traffic = Clash.openTrafficEvent().apply {
                onEvent {
                    setSpeed(it.up, it.down)
                }
            }
        }
    }

    private fun disableUpdate() {
        handler.post {
            if (traffic == null)
                return@post

            traffic?.close()
            traffic = null
        }
    }

    private fun update() {
        if (showing)
            context.startForeground(CLASH_STATUS_NOTIFICATION_ID, createNotification())
    }

    private fun createNotification(): Notification {
        return baseBuilder
            .setContentTitle(profile)
            .setContentText(
                context.getString(
                    R.string.clash_notification_content,
                    ByteFormatter.byteToStringSecond(up),
                    ByteFormatter.byteToStringSecond(down)
                )
            )
            .setSubText(if (vpn) context.getText(R.string.clash_service_vpn_mode) else null)
            .build()
    }
}