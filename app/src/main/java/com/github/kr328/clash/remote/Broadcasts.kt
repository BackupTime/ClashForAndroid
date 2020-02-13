package com.github.kr328.clash.remote

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.github.kr328.clash.service.ClashService
import com.github.kr328.clash.service.Intents
import com.github.kr328.clash.service.data.ClashProfileEntity
import com.github.kr328.clash.service.util.componentName

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
            if ( intent?.`package` != context?.packageName )
                return

            when (intent?.action) {
                Intents.INTENT_ACTION_CLASH_STARTED ->
                    receivers.forEach {
                        it.onStarted()
                    }
                Intents.INTENT_ACTION_CLASH_STOPPED ->
                    receivers.forEach {
                        it.onStopped(intent.getStringExtra(Intents.INTENT_EXTRA_CLASH_STOP_REASON))
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

                clashRunning = broadcastReceiver.peekService(
                    application,
                    Intent().setComponent(ClashService::class.componentName)
                ) != null
            }

            override fun onStop(owner: LifecycleOwner) {
                application.unregisterReceiver(broadcastReceiver)

                clashRunning = false
            }
        })
    }
}