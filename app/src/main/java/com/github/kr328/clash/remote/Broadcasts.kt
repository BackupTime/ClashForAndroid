package com.github.kr328.clash.remote

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.github.kr328.clash.service.Constants
import com.github.kr328.clash.service.Intents
import com.github.kr328.clash.service.ServiceStatusProvider
import com.github.kr328.clash.service.data.ClashProfileEntity

object Broadcasts {
    interface Receiver {
        fun onStarted()
        fun onStopped(cause: String?)
        fun onProfileChanged(active: ClashProfileEntity?)
    }

    var clashRunning: Boolean = false

    private val receivers = mutableListOf<Receiver>()
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.`package` != context?.packageName)
                return

            when (intent?.action) {
                Intents.INTENT_ACTION_CLASH_STARTED -> {
                    clashRunning = true

                    receivers.forEach {
                        it.onStarted()
                    }
                }
                Intents.INTENT_ACTION_CLASH_STOPPED -> {
                    clashRunning = false

                    receivers.forEach {
                        it.onStopped(intent.getStringExtra(Intents.INTENT_EXTRA_CLASH_STOP_REASON))
                    }
                }
                Intents.INTENT_ACTION_PROFILE_CHANGED ->
                    receivers.forEach {
                        it.onProfileChanged(intent.getParcelableExtra(Intents.INTENT_EXTRA_PROFILE_ACTIVE))
                    }
            }
        }
    }

    fun register(receiver: Receiver) {
        receivers.add(receiver)
    }

    fun unregister(receiver: Receiver) {
        receivers.remove(receiver)
    }

    fun init(application: Application) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                application.registerReceiver(broadcastReceiver, IntentFilter().apply {
                    addAction(Intents.INTENT_ACTION_PROFILE_CHANGED)
                    addAction(Intents.INTENT_ACTION_CLASH_STOPPED)
                    addAction(Intents.INTENT_ACTION_CLASH_STARTED)
                })

                val pong = application.contentResolver.call(
                    "${application.packageName}${Constants.STATUS_PROVIDER_SUFFIX}",
                    ServiceStatusProvider.METHOD_PING_CLASH_SERVICE,
                    null,
                    null
                )

                clashRunning = pong != null
            }

            override fun onStop(owner: LifecycleOwner) {
                application.unregisterReceiver(broadcastReceiver)

                clashRunning = false
            }
        })
    }
}