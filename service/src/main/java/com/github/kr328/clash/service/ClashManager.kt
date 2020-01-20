package com.github.kr328.clash.service

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.General
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.service.ipc.ParcelableCompletedFuture
import com.github.kr328.clash.service.ipc.ParcelablePipe

class ClashManager(context: Context): IClashManager.Stub() {
    private val settings = context.getSharedPreferences("service", Context.MODE_PRIVATE)

    override fun queryAllProxies(): Array<ProxyGroup> {
        return Clash.queryProxyGroups().toTypedArray()
    }

    override fun queryGeneral(): General {
        return Clash.queryGeneral()
    }

    override fun setSelectProxy(proxy: String?, selected: String?): Boolean {
        require(proxy != null && selected != null)

        return Clash.setSelectedProxy(proxy, selected)
    }

    override fun openBandwidthEvent(): ParcelablePipe {
        return object: ParcelablePipe() {
            val stream = Clash.openBandwidthEvent().apply {
                onEvent {
                    send(it)
                }
            }

            override fun onClose() {
                stream.close()
            }
        }
    }

    override fun startHealthCheck(group: String?): ParcelableCompletedFuture {
        require(group != null)

        return ParcelableCompletedFuture().apply {
            Clash.startHealthCheck(group).whenComplete { _: Unit?, u: Throwable? ->
                if ( u != null )
                    this.completeExceptionally(u)
                else
                    this.complete(null)
            }
        }
    }

    override fun openLogEvent(): ParcelablePipe {
        return object: ParcelablePipe() {
            val stream = Clash.openLogEvent().apply {
                onEvent {
                    send(it)
                }
            }

            override fun onClose() {
                stream.close()
            }
        }
    }

    override fun putSetting(key: String?, value: String?): Boolean {
        settings.edit {
            putString(key, value)
        }
        return true
    }

    override fun getSetting(key: String?): String {
        return settings.getString(key, "")!!
    }
}