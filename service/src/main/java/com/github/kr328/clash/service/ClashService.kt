package com.github.kr328.clash.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import bridge.Bridge
import com.github.kr328.clash.callback.IUrlTestCallback
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.event.*
import com.github.kr328.clash.core.model.GeneralPacket
import com.github.kr328.clash.core.model.ProxyPacket
import com.github.kr328.clash.core.utils.Log
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import javax.xml.transform.SourceLocator
import kotlin.concurrent.thread

class ClashService : Service(), IClashEventObserver, ClashEventService.Master,
    ClashProfileService.Master, ClashEventPuller.Master {
    private val executor = Executors.newSingleThreadExecutor()

    private val eventService = ClashEventService(this)
    private val profileService = ClashProfileService(this, this)
    private val settingService = ClashSettingService(this)

    private var processStatus = ProcessEvent.STOPPED

    //private lateinit var puller: ClashEventPuller
    private lateinit var notification: ClashNotification

    private val clashService = object : IClashService.Stub() {
        override fun setSelectProxy(proxy: String?, selected: String?) {
            require(proxy != null && selected != null)

            try {
                this@ClashService.profileService.setCurrentProfileProxy(proxy, selected)
            } catch (e: IOException) {
                Log.w("Set proxy failure", e)

                this@ClashService.eventService.performErrorEvent(
                    ErrorEvent(ErrorEvent.Type.SET_PROXY_SELECTED, e.toString())
                )
            }
        }

        override fun queryGeneral(): GeneralPacket {
            return GeneralPacket(GeneralPacket.Ports(0, 0, 0, 0), GeneralPacket.Mode.DIRECT)
        }

        override fun queryAllProxies(): ProxyPacket {
            return ProxyPacket("Direct", emptyMap())
        }

        override fun startUrlTest(proxies: Array<out String>?, callback: IUrlTestCallback?) {
            require(proxies != null && callback != null)
        }

        override fun start() {
            if ( processStatus == ProcessEvent.STARTED )
                return

            processStatus = ProcessEvent.STARTED

            this@ClashService.eventService.performProcessEvent(processStatus)
        }

        override fun stop() {
            if ( processStatus == ProcessEvent.STOPPED )
                return

            processStatus = ProcessEvent.STOPPED

            this@ClashService.eventService.performProcessEvent(processStatus)
        }

        override fun getEventService(): IClashEventService {
            return this@ClashService.eventService
        }

        override fun getProfileService(): IClashProfileService {
            return this@ClashService.profileService
        }

        override fun getSettingService(): IClashSettingService {
            return this@ClashService.settingService
        }

        override fun getCurrentProcessStatus(): ProcessEvent {
            return processStatus
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON ->
                    eventService.registerEventObserver(
                        ClashService::class.java.name,
                        this@ClashService,
                        intArrayOf(Event.EVENT_SPEED)
                    )
                Intent.ACTION_SCREEN_OFF ->
                    eventService.registerEventObserver(
                        ClashService::class.java.name,
                        this@ClashService,
                        intArrayOf()
                    )
            }
        }
    }

    override fun acquireEvent(event: Int) {
        if (processStatus == ProcessEvent.STOPPED)
            return

    }

    override fun releaseEvent(event: Int) {
        if (processStatus == ProcessEvent.STOPPED)
            return
    }

    override fun onCreate() {
        super.onCreate()

        Bridge.init(filesDir.resolve("clash").absolutePath)

        //puller = ClashEventPuller(clash, this)

        notification = ClashNotification(this)

        eventService.registerEventObserver(
            ClashService::class.java.name,
            this@ClashService,
            intArrayOf(Event.EVENT_SPEED)
        )

        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        clashService.start()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return clashService
    }

    override fun onDestroy() {
        clashService.stop()

        executor.shutdown()
        eventService.shutdown()

        unregisterReceiver(screenReceiver)

        super.onDestroy()
    }

    override fun onProfileChanged(event: ProfileChangedEvent?) {
        reloadProfile()
    }

    override fun onProcessEvent(event: ProcessEvent?) {
        when (event!!) {
            ProcessEvent.STARTED -> {
                reloadProfile()

                notification.show()

                eventService.recastEventRequirement()
            }
            ProcessEvent.STOPPED -> {
                eventService.performSpeedEvent(SpeedEvent(0, 0))
                eventService.performBandwidthEvent(BandwidthEvent(0))

                notification.cancel()

                stopSelf()

                Bridge.loadProfileDefault()
            }
        }

        sendBroadcast(Intent(Constants.CLASH_PROCESS_BROADCAST_ACTION).setPackage(packageName))
    }

    private fun reloadProfile() {
        executor.submit {
            if ( processStatus != ProcessEvent.STARTED)
                return@submit

            val active = profileService.queryActiveProfile()

            if (active == null) {
                eventService.performErrorEvent(ErrorEvent(ErrorEvent.Type.PROFILE_LOAD, "No profile activated"))
                clashService.stop()
                return@submit
            }

            Log.i("Loading profile ${active.file}")

            try {
                Bridge.loadProfileFile(active.file)

                notification.setProfile(active.name)

                eventService.performProfileReloadEvent(ProfileReloadEvent())
            } catch (e: Exception) {
                clashService.stop()
                eventService.performErrorEvent(ErrorEvent(ErrorEvent.Type.PROFILE_LOAD, e.message ?: "Unknown"))
                Log.w("Load profile failure", e)
            }
        }
    }

    override fun onProfileReloaded(event: ProfileReloadEvent?) {
        sendBroadcast(Intent(Constants.CLASH_RELOAD_BROADCAST_ACTION).setPackage(packageName))
    }

    override fun onSpeedEvent(event: SpeedEvent?) {
        notification.setSpeed(event?.up ?: 0, event?.down ?: 0)
    }

    override fun preformProfileChanged() {
        eventService.performProfileChangedEvent(ProfileChangedEvent())
    }

    override fun onLogPulled(event: LogEvent) {
        eventService.performLogEvent(event)
    }

    override fun onSpeedPulled(event: SpeedEvent) {
        eventService.performSpeedEvent(event)
    }

    override fun onBandwidthPulled(event: BandwidthEvent) {
        eventService.performBandwidthEvent(event)
    }

    override fun onBandwidthEvent(event: BandwidthEvent?) {}
    override fun onLogEvent(event: LogEvent?) {}
    override fun onErrorEvent(event: ErrorEvent?) {}
    override fun asBinder(): IBinder = object : Binder() {
        override fun queryLocalInterface(descriptor: String): IInterface? {
            return this@ClashService
        }
    }
}