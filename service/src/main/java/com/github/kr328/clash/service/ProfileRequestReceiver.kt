package com.github.kr328.clash.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.kr328.clash.service.transact.ProfileRequest
import com.github.kr328.clash.service.util.componentName
import com.github.kr328.clash.service.util.startForegroundServiceCompat

class ProfileRequestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Intents.INTENT_ACTION_PROFILE_ENQUEUE_REQUEST || context == null)
            return

        val id = intent.getLongExtra(Intents.INTENT_EXTRA_PROFILE_ID, -1)
        if ( id < 0 )
            return

        val request = ProfileRequest()
            .action(ProfileRequest.Action.UPDATE_OR_CREATE)
            .withId(id)

        val service = Intent(Intents.INTENT_ACTION_PROFILE_ENQUEUE_REQUEST)
            .setComponent(ProfileBackgroundService::class.componentName)
            .putExtra(Intents.INTENT_EXTRA_PROFILE_REQUEST, request)

        context.startForegroundServiceCompat(service)
    }
}