package com.github.kr328.clash.service.clash.module

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.github.kr328.clash.component.ids.Intents

class CloseModule(private val service: Service) : Module {
    private var callback: () -> Unit = {}
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            callback()
        }
    }

    fun onClose(cb: () -> Unit) {
        callback = cb
    }

    override suspend fun onCreate() {}

    override suspend fun onStart() {
        service.registerReceiver(receiver, IntentFilter(Intents.INTENT_ACTION_REQUEST_STOP))
    }

    override suspend fun onStop() {
        service.unregisterReceiver(receiver)
    }

    override suspend fun onDestroy() {

    }
}