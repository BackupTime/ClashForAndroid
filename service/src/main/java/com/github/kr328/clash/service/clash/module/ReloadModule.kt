package com.github.kr328.clash.service.clash.module

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.github.kr328.clash.component.ids.Intents
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.data.ClashDatabase
import com.github.kr328.clash.service.util.resolveBase
import com.github.kr328.clash.service.util.resolveProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ReloadModule(private val context: Context) : Module {
    private lateinit var backgroundJob: Job
    private var emptyProfileCallback: () -> Unit = {}
    private val reloadChannel = Channel<Unit>(Channel.CONFLATED)
    private val reloadBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            reloadChannel.offer(Unit)
        }
    }

    override suspend fun onCreate() {
        backgroundJob = launch {
            val database = ClashDatabase.getInstance(context).openClashProfileDao()

            while (isActive) {
                reloadChannel.receive()

                val id = database.queryActiveProfile()?.id
                if (id == null) {
                    emptyProfileCallback()
                    continue
                }

                Clash.loadProfile(resolveProfile(id), resolveBase(id))
            }
        }
    }

    override suspend fun onStart() {
        context.registerReceiver(
            reloadBroadcastReceiver,
            IntentFilter().apply {
                addAction(Intents.INTENT_ACTION_PROFILE_CHANGED)
                addAction(Intents.INTENT_ACTION_NETWORK_CHANGED)
            }
        )

        reloadChannel.offer(Unit)
    }

    override suspend fun onStop() {
        context.unregisterReceiver(reloadBroadcastReceiver)
    }

    override suspend fun onDestroy() {
        backgroundJob.cancel()
    }

    fun onEmptyProfile(callback: () -> Unit) {
        emptyProfileCallback = callback
    }
}