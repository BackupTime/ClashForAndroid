package com.github.kr328.clash.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.github.kr328.clash.common.ids.Intents
import com.github.kr328.clash.common.ids.PendingIds
import com.github.kr328.clash.service.data.ProfileDao
import com.github.kr328.clash.service.model.toProfileMetadata
import com.github.kr328.clash.common.util.componentName
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.startForegroundServiceCompat

class ProfileReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        when (intent?.action) {
            Intents.INTENT_ACTION_PROFILE_REQUEST_UPDATE -> {
                // Redirect to service
                intent.component = ProfileBackgroundService::class.componentName
                context.startForegroundServiceCompat(intent)
            }
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                tryInitialize(context)
            }
        }
    }

    companion object {
        private val requested = mutableMapOf<Long, PendingIntent>()
        private var initialized = false

        @Synchronized
        fun tryInitialize(context: Context) {
            if ( initialized )
                return
            initialized = true

            context.startForegroundServiceCompat(
                ProfileBackgroundService::class.intent
                    .setAction(Intents.INTENT_ACTION_PROFILE_SETUP)
            )
        }

        suspend fun requestNextUpdate(context: Context, id: Long) {
            val metadata = ProfileDao.queryById(id)?.toProfileMetadata(context) ?: return
            val service = context.getSystemService<AlarmManager>() ?: return

            if (metadata.interval <= 0)
                return

            cancelNextUpdate(context, id)

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                PendingIds.generateProfileResultId(id),
                Intent(Intents.INTENT_ACTION_PROFILE_REQUEST_UPDATE)
                    .setComponent(ProfileReceiver::class.componentName)
                    .putExtra(Intents.INTENT_EXTRA_PROFILE_ID, id),
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            service.set(
                AlarmManager.RTC,
                metadata.lastModified + metadata.interval,
                pendingIntent
            )

            requested[id] = pendingIntent
        }

        fun cancelNextUpdate(context: Context, id: Long) {
            val service = context.getSystemService<AlarmManager>() ?: return
            service.cancel(requested.remove(id) ?: return)
        }
    }
}