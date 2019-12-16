package com.github.kr328.clash.service

import com.github.kr328.clash.callback.IUrlTestCallback
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.event.ErrorEvent
import com.github.kr328.clash.core.event.ProcessEvent
import com.github.kr328.clash.core.model.CompressedProxyList
import com.github.kr328.clash.core.model.General
import com.github.kr328.clash.core.model.compress
import com.github.kr328.clash.core.utils.Log
import java.io.IOException

class ClashServiceImpl(val clashService: ClashService) : IClashService.Stub() {
    private val bridge: ClashEventBridge = ClashEventBridge(clashService)

    val clash: Clash = Clash(clashService, bridge::onProcessChanged)
    val profileService = ClashProfileService(clashService, bridge)
    val settingService = ClashSettingService(clashService)
    val eventService: ClashEventService
        get() = bridge.eventService

    override fun setSelectProxy(proxy: String?, selected: String?) {
        require(proxy != null && selected != null)

        try {
            profileService.setCurrentProfileProxy(proxy, selected)
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

    fun getClashInstance(): Clash {
        return clash
    }

    fun shutdown() {
        eventService.shutdown()
    }
}