package com.github.kr328.clash.service.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.github.kr328.clash.common.ids.Intents
import com.github.kr328.clash.service.ProfileRequestReceiver
import com.github.kr328.clash.service.data.ClashProfileEntity

object UpdateUtils {
    fun resetProfileUpdateAlarm(context: Context, profile: ClashProfileEntity) {
        if (profile.updateInterval > 0) {
            requireNotNull(context.getSystemService(AlarmManager::class.java)).set(
                AlarmManager.RTC,
                profile.lastUpdate + (profile.updateInterval * 1000),
                PendingIntent.getBroadcast(
                    context,
                    RandomUtils.nextInt(),
                    Intent(Intents.INTENT_ACTION_PROFILE_ENQUEUE_REQUEST)
                        .setComponent(ProfileRequestReceiver::class.componentName)
                        .putExtra(Intents.INTENT_EXTRA_PROFILE_ID, profile.id),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        }
    }
}