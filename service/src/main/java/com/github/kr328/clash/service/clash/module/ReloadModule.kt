package com.github.kr328.clash.service.clash.module

import android.content.Context
import android.content.Intent
import com.github.kr328.clash.common.ids.Intents
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.ServiceStatusProvider
import com.github.kr328.clash.service.data.ClashDatabase
import com.github.kr328.clash.service.util.resolveBase
import com.github.kr328.clash.service.util.resolveProfile
import kotlinx.coroutines.sync.Mutex
import java.lang.NullPointerException

class ReloadModule(private val context: Context) : Module() {
    override val receiveBroadcasts: Set<String>
        get() = setOf(Intents.INTENT_ACTION_NETWORK_CHANGED, Intents.INTENT_ACTION_PROFILE_CHANGED)
    private val reloadMutex = Mutex()
    private var loadedCallback: (Exception?) -> Unit = {}

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


    fun onLoaded(callback: (Exception?) -> Unit) {
        loadedCallback = callback
    }

    private suspend fun reload() {
        try {
            val database = ClashDatabase.getInstance(context)
            val profileDao = database.openClashProfileDao()
            val proxyDao = database.openClashProfileProxyDao()

            val active = profileDao.queryActiveProfile()
                ?: throw NullPointerException("No profile selected")

            Clash.loadProfile(resolveProfile(active.id), resolveBase(active.id)).await()

            val remove = proxyDao.querySelectedForProfile(active.id)
                .filterNot { Clash.setSelectedProxy(it.proxy, it.selected) }
                .map { it.selected }

            proxyDao.removeSelectedForProfile(active.id, remove)

            ServiceStatusProvider.currentProfile = active.name

            loadedCallback(null)
        } catch (e: Exception) {
            loadedCallback(e)
        }
    }
}