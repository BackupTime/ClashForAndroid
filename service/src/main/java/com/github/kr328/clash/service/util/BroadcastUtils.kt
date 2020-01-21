package com.github.kr328.clash.service.util

import android.content.Context
import android.content.Intent

fun Context.sendBroadcastSelf(intent: Intent) {
    this.sendBroadcast(intent.setPackage(this.packageName))
}