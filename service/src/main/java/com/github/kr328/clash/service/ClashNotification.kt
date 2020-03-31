package com.github.kr328.clash.service

import android.app.*
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.utils.asBytesString
import com.github.kr328.clash.core.utils.asSpeedString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClashNotification(
    private val context: Service
) {
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
        .setGroup(CLASH_STATUS_NOTIFICATION_CHANNEL)
        .setContentIntent(
            PendingIntent.getActivity(
                context,
                CLASH_STATUS_NOTIFICATION_ID,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        )

    private var currentProfile = "None"

    init {
        createNotificationChannel()

        updateBase()
    }

    fun destroy() {
        context.stopForeground(true)

        updateDestroy()
    }

    fun setProfile(profile: String) {
        currentProfile = profile
    }

    suspend fun update() {
        val notification = withContext(Dispatchers.Default) {
            createNotification()
        }

        context.startForeground(CLASH_STATUS_NOTIFICATION_ID, notification)
    }

    fun updateBase() {
        val notification = baseBuilder
            .setContentTitle(currentProfile)
            .setContentText(context.getText(R.string.running))
            .build()

        context.startForeground(CLASH_STATUS_NOTIFICATION_ID, notification)
    }

    private fun updateDestroy() {
        // just waiting system cancel our notification :)
        // fxxking google

        val notification = baseBuilder
            .setContentTitle(context.getText(R.string.destroying))
            .setContentText(context.getText(R.string.recycling_resources))
            .setSubText(null)
            .build()

        NotificationManagerCompat.from(context).apply {
            notify(CLASH_STATUS_NOTIFICATION_ID, notification)
            cancel(CLASH_STATUS_NOTIFICATION_ID)
        }
    }

    private fun createNotification(): Notification {
        val traffic = Clash.queryTraffic()
        val bandwidth = Clash.queryBandwidth()

        return baseBuilder
            .setContentTitle(currentProfile)
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