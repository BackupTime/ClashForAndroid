package com.github.kr328.clash.utils

import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.github.kr328.clash.preference.UiSettings
import com.github.kr328.clash.service.ClashService
import com.github.kr328.clash.service.Intents
import com.github.kr328.clash.service.TunService
import com.github.kr328.clash.service.util.intent
import com.github.kr328.clash.service.util.sendBroadcastSelf
import com.github.kr328.clash.service.util.startForegroundServiceCompat

fun Context.startClashService(): Intent? {
    val startTun = UiSettings(this).get(UiSettings.ENABLE_VPN)

    if (startTun) {
        val vpnRequest = VpnService.prepare(this)
        if (vpnRequest != null)
            return vpnRequest

        startForegroundServiceCompat(TunService::class.intent)
    } else {
        startForegroundServiceCompat(ClashService::class.intent)
    }

    return null
}

fun Context.stopClashService() {
    sendBroadcastSelf(Intent(Intents.INTENT_ACTION_REQUEST_STOP))
}