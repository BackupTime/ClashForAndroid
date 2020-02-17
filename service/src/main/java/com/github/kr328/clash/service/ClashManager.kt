package com.github.kr328.clash.service

import android.content.Context
import androidx.core.content.edit
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.General
import com.github.kr328.clash.core.model.ProxyGroupList
import com.github.kr328.clash.service.data.ClashDatabase
import com.github.kr328.clash.service.data.ClashProfileProxyEntity
import com.github.kr328.clash.service.ipc.IStreamCallback
import com.github.kr328.clash.service.ipc.ParcelableContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ClashManager(context: Context, parent: CoroutineScope) :
    IClashManager.Stub(), CoroutineScope by parent {
    private val settings = context.getSharedPreferences("service", Context.MODE_PRIVATE)
    private val database = ClashDatabase.getInstance(context)

    override fun setProxyMode(mode: String?) {
        Clash.setProxyMode(requireNotNull(mode))
    }

    override fun queryAllProxies(): ProxyGroupList {
        return ProxyGroupList(Clash.queryProxyGroups())
    }

    override fun queryGeneral(): General {
        return Clash.queryGeneral()
    }

    override fun setSelectProxy(proxy: String?, selected: String?): Boolean {
        require(proxy != null && selected != null)

        launch {
            val current = database.openClashProfileDao()
                .queryActiveProfile() ?: return@launch
            database.openClashProfileProxyDao()
                .setSelectedForProfile(ClashProfileProxyEntity(current.id, proxy, selected))
        }

        return Clash.setSelectedProxy(proxy, selected)
    }

    override fun putSetting(key: String?, value: String?): Boolean {
        settings.edit(commit = false) {
            putString(key, value)
        }
        return true
    }

    override fun queryBandwidth(): Long {
        val data = Clash.queryBandwidth()

        return data.download + data.upload
    }

    override fun openLogEvent(callback: IStreamCallback?) {
        require(callback != null)

        Clash.openLogEvent().apply {
            callback.asBinder()?.linkToDeath({
                close()
            }, 0)

            onEvent {
                try {
                    callback.send(ParcelableContainer(it))
                } catch (e: Exception) {
                    close()
                }
            }
        }
    }

    override fun startHealthCheck(group: String?, callback: IStreamCallback?) {
        require(group != null && callback != null)

        Clash.startHealthCheck(group).invokeOnCompletion { u ->
            if (u != null)
                callback.completeExceptionally(u.message)
            else
                callback.complete()
        }
    }

    override fun getSetting(key: String?): String? {
        return settings.getString(key, null)
    }
}