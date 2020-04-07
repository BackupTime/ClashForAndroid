package com.github.kr328.clash.service.clash.module

import android.content.Context
import android.content.Intent
import com.github.kr328.clash.common.ids.Intents
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.data.ClashDatabase
import com.github.kr328.clash.service.util.resolveBase
import com.github.kr328.clash.service.util.resolveProfile
import kotlinx.coroutines.sync.Mutex

class ReloadModule(private val context: Context) : Module() {
    override val receiveBroadcasts: Set<String>
        get() = setOf(Intents.INTENT_ACTION_NETWORK_CHANGED, Intents.INTENT_ACTION_PROFILE_CHANGED)
    private val reloadMutex = Mutex()
    private var emptyCallback: () -> Unit = {}

    override suspend fun onStart() {
        reload()
    }

    override suspend fun onBroadcastReceived(intent: Intent) {
        if (!reloadMutex.tryLock())
            return

        when (intent.action) {
            Intents.INTENT_ACTION_NETWORK_CHANGED, Intents.INTENT_ACTION_PROFILE_CHANGED -> {
                reload()
            }
        }

        reloadMutex.unlock()
    }

    fun onEmpty(callback: () -> Unit) {
        emptyCallback = callback
    }

    private suspend fun reload() {
        val database = ClashDatabase.getInstance(context).openClashProfileDao()

        val id = database.queryActiveProfile()?.id
        if (id == null) {
            emptyCallback()
            reloadMutex.unlock()
            return
        }

        Clash.loadProfile(resolveProfile(id), resolveBase(id))
    }
}