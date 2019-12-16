package com.github.kr328.clash.service

import android.os.ParcelFileDescriptor
import com.github.kr328.clash.callback.IUrlTestCallback
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.event.ErrorEvent
import com.github.kr328.clash.core.event.ProcessEvent
import com.github.kr328.clash.core.model.CompressedProxyList
import com.github.kr328.clash.core.model.General
import com.github.kr328.clash.core.model.compress
import com.github.kr328.clash.core.utils.Log
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class ClashServiceImpl(clashService: ClashService) : IClashService.Stub() {
    private val bridge: ClashEventBridge = ClashEventBridge(clashService)

    val clash: Clash = Clash(clashService, bridge::onProcessChanged)
    val profileService = ClashProfileService(clashService, bridge)
    val settingService = ClashSettingService(clashService)
    val eventPoll = ClashEventPoll(clash, bridge)
    val eventService: ClashEventService
        get() = bridge.eventService

    override fun setSelectProxy(proxy: String?, selected: String?) {
        require(proxy != null && selected != null)

        try {
            if ( clash.setSelectedProxy(proxy, selected) )
                profileService.setCurrentProfileProxy(proxy, selected)
            else
                eventService.performErrorEvent(
                    ErrorEvent(ErrorEvent.Type.SET_PROXY_SELECTED, "Unable to set $proxy -> $selected")
                )
        } catch (e: IOException) {
            Log.w("Set proxy failure", e)

            eventService.performErrorEvent(
                ErrorEvent(ErrorEvent.Type.SET_PROXY_SELECTED, e.toString())
            )
        }
    }

    override fun queryGeneral(): General {
        return clash.queryGeneral()
    }

    override fun queryAllProxies(): CompressedProxyList {
        return try {
            clash.queryProxies().compress()
        }
        catch (e: Exception) {
            Log.w("Query proxies", e)

            eventService.performErrorEvent(ErrorEvent(ErrorEvent.Type.QUERY_PROXY_FAILURE, e.message ?: "Unknown"))

            CompressedProxyList(emptyMap(), emptyList())
        }
    }

    override fun startUrlTest(proxies: Array<out String>?, callback: IUrlTestCallback?) {
        require(proxies != null && callback != null)

        val count = AtomicInteger(proxies.size)

        proxies.forEach {
            clash.startUrlTest(it) { n, d ->
                callback.onResult(n, d)

                count.getAndDecrement()

                if ( count.get() == 0 )
                    callback.onResult(null, 0)
            }
        }
    }

    override fun checkProfileValid(pipe: ParcelFileDescriptor?): String? {
        require(pipe != null)

        val data = FileInputStream(pipe.fileDescriptor).use {
            String(it.readBytes())
        }

        pipe.close()

        return clash.checkProfileValid(data)
    }

    override fun start() {
        clash.start()
    }

    override fun stop() {
        clash.stop()
    }

    override fun getEventService(): IClashEventService {
        return eventService
    }

    override fun getProfileService(): IClashProfileService {
        return profileService
    }

    override fun getSettingService(): IClashSettingService {
        return settingService
    }

    override fun getCurrentProcessStatus(): ProcessEvent {
        return clash.getCurrentProcessStatus()
    }

    fun shutdown() {
        eventService.shutdown()
    }
}