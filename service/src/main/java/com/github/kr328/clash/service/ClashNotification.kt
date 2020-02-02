package com.github.kr328.clash.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.utils.asSpeedString

class ClashNotification(private val context: Service) {
    companion object {
        private const val CLASH_STATUS_NOTIFICATION_CHANNEL = "clash_status_channel"
        private const val CLASH_STATUS_NOTIFICATION_ID = 413
    }

    private val handler = Handler()
    private var showing = false

    private val contentIntent = Intent(Intent.ACTION_MAIN)
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

    private var auto = false
    private var vpn = false
    private var profile = "None"
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
                        NotificationManager.IMPORTANCE_LOW
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

    private fun updateSpeed() {
        handler.postDelayed({
            if (!auto)
                return@postDelayed

            update()

            updateSpeed()
        }, 1000)
    }

    private fun enableUpdate() {
        handler.post {
            if (auto)
                return@post

            auto = true

            updateSpeed()
        }
    }

    private fun disableUpdate() {
        handler.post {
            auto = false
        }
    }

    private fun update() {
        if (showing)
            context.startForeground(CLASH_STATUS_NOTIFICATION_ID, createNotification())
    }

    private fun createNotification(): Notification {
        val traffic = Clash.queryTrafficEvent()

        return baseBuilder
            .setContentTitle(profile)
            .setContentText(
                context.getString(
                    R.string.clash_notification_content,
                    traffic.upload.asSpeedString(),
                    traffic.download.asSpeedString()
                )
            )
            .setSubText(if (vpn) context.getText(R.string.clash_service_vpn_mode) else null)
            .build()
    }
}