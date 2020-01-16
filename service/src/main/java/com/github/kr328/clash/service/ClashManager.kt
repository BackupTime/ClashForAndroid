package com.github.kr328.clash.service

import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.General
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.service.ipc.ParcelableCompletedFuture
import com.github.kr328.clash.service.ipc.ParcelablePipe

class ClashManager(val clash: Clash): IClashManager.Stub() {
    override fun updateProfile(id: Int): ParcelableCompletedFuture {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addProfile(url: String?): ParcelableCompletedFuture {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queryAllProxies(): Array<ProxyGroup> {
        return clash.queryProxyGroups().toTypedArray()
    }

    override fun queryGeneral(): General {
        return clash.queryGeneral()
    }

    override fun setSelectProxy(proxy: String?, selected: String?): Boolean {
        require(proxy != null && selected != null)

        return clash.setSelectedProxy(proxy, selected)
    }

    override fun openBandwidthEvent(): ParcelablePipe {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queryAllProfiles(): ParcelableCompletedFuture {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun startHealthCheck(group: String?): ParcelableCompletedFuture {
        require(group != null)

        return ParcelableCompletedFuture().apply {
            clash.startHealthCheck(group).whenComplete { _: Unit?, u: Throwable? ->
                if ( u != null )
                    this.completeExceptionally(u)
                else
                    this.complete(null)
            }
        }
    }

    override fun openLogEvent(): ParcelablePipe {
        return object: ParcelablePipe() {
            val stream = clash.openLogEvent().apply {
                onEvent {
                    send(it)
                }
            }

            override fun onClose() {
                stream.close()
            }
        }
    }
}