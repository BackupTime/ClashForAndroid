package com.github.kr328.clash.service.util

import android.content.Context
import android.content.Intent
import com.github.kr328.clash.core.utils.Log
import com.github.kr328.clash.service.Intents
import com.github.kr328.clash.service.data.ClashDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Context.sendBroadcastSelf(intent: Intent) {
    this.sendBroadcast(intent.setPackage(this.packageName))
}

suspend fun broadcastProfileChanged(context: Context) {
    val active = ClashDatabase.getInstance(context).openClashProfileDao().queryActiveProfile()
    val intent = Intent(Intents.INTENT_ACTION_PROFILE_CHANGED)
        .putExtra(Intents.INTENT_EXTRA_PROFILE_ACTIVE, active)

    context.sendBroadcastSelf(intent)

    Log.d("Broadcasting $intent")
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