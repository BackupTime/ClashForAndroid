package com.github.kr328.clash.remote

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.github.kr328.clash.service.Intents

object Broadcasts {
    interface Receiver {
        fun onStarted()
        fun onStopped(cause: String?)
        fun onProfileChanged()
        fun onProfileLoaded()
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
                        it.onProfileChanged()
                    }
                Intents.INTENT_ACTION_PROFILE_LOADED -> {
                    receivers.forEach {
                        it.onProfileLoaded()
                    }
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
                    addAction(Intents.INTENT_ACTION_PROFILE_LOADED)
                })


                val current = RemoteUtils.detectClashRunning(application)
                if (current != clashRunning) {
                    clashRunning = current

                    if (current) {
                        receivers.forEach {
                            it.onStarted()
                        }
                    } else {
                        receivers.forEach {
                            it.onStopped(null)
                        }
                    }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                application.unregisterReceiver(broadcastReceiver)
            }
        })
    }
}