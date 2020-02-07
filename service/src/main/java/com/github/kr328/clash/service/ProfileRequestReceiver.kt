package com.github.kr328.clash.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.github.kr328.clash.service.util.componentName

class ProfileRequestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Intents.INTENT_ACTION_PROFILE_ENQUEUE_REQUEST || context == null)
            return

        intent.component = ProfileProcessService::class.componentName

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(intent)
        else
            context.startService(intent)
    }
}