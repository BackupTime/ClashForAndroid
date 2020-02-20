package com.github.kr328.clash.service.util

import android.content.Context
import android.content.Intent
import com.github.kr328.clash.service.Intents

fun Context.sendBroadcastSelf(intent: Intent) {
    this.sendBroadcast(intent.setPackage(this.packageName))
}

fun broadcastProfileChanged(context: Context) {
    val intent = Intent(Intents.INTENT_ACTION_PROFILE_CHANGED)

    context.sendBroadcastSelf(intent)
}

fun broadcastProfileLoaded(context: Context) {
    val intent = Intent(Intents.INTENT_ACTION_PROFILE_LOADED)

    context.sendBroadcastSelf(intent)
}

fun broadcastNetworkChanged(context: Context) {
    context.sendBroadcastSelf(Intent(Intents.INTENT_ACTION_NETWORK_CHANGED))
}

fun broadcastClashStarted(context: Context) {
    context.sendBroadcastSelf(Intent(Intents.INTENT_ACTION_CLASH_STARTED))
}

fun broadcastClashStopped(context: Context, reason: String?) {
    context.sendBroadcastSelf(
        Intent(Intents.INTENT_ACTION_CLASH_STOPPED).putExtra(
            Intents.INTENT_EXTRA_CLASH_STOP_REASON,
            reason
        )
    )
}