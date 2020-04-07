package com.github.kr328.clash

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.kr328.clash.common.ids.Intents
import com.github.kr328.clash.service.ProfileBackgroundService
import com.github.kr328.clash.service.util.componentName
import com.github.kr328.clash.service.util.startForegroundServiceCompat
import com.github.kr328.clash.utils.startClashService

class OnBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED || context == null)
            return

        context.startClashService()
        context.startForegroundServiceCompat(
            Intent(Intents.INTENT_ACTION_PROFILE_SETUP)
                .setComponent(ProfileBackgroundService::class.componentName)
        )
    }
}