package com.github.kr328.clash.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.event.*
import com.github.kr328.clash.core.utils.Log
import java.util.concurrent.Executors

class ClashService : Service(), IClashEventObserver {
    private val executor = Executors.newSingleThreadExecutor()

    private val instance: ClashServiceImpl by lazy {
        ClashServiceImpl(this)
    }

    val events: ClashEventService
        get() = instance.eventService
    val clash: Clash
        get() = instance.clash

    //private lateinit var puller: ClashEventPuller
    private lateinit var notification: ClashNotification

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON ->
                    instance.eventService.registerEventObserver(
                        ClashService::class.java.name,
                        this@ClashService,
                        intArrayOf(Event.EVENT_SPEED)
                    )
                Intent.ACTION_SCREEN_OFF ->
                    instance.eventService.registerEventObserver(
                        ClashService::class.java.name,
                        this@ClashService,
                        intArrayOf()
                    )
            }
        }
    }

    private fun onClashProcessChanged(event: ProcessEvent) {
        instance.eventService.performProcessEvent(event)
    }

    fun acquireEvent(event: Int) {
        if ( instance.clash.getCurrentProcessStatus() == ProcessEvent.STOPPED )
            return


    }

    fun releaseEvent(event: Int) {
        if ( instance.clash.getCurrentProcessStatus() == ProcessEvent.STOPPED )
            return


    }

    override fun onCreate() {
        super.onCreate()

        //puller = ClashEventPuller(clash, this)

        notification = ClashNotification(this)

        instance.eventService.registerEventObserver(
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

        instance.clash.start()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return instance
    }

    override fun onDestroy() {
        instance.clash.stop()

        executor.shutdown()

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

                instance.eventService.recastEventRequirement()
            }
            ProcessEvent.STOPPED -> {
                instance.eventService.performSpeedEvent(SpeedEvent(0, 0))
                instance.eventService.performBandwidthEvent(BandwidthEvent(0))

                notification.cancel()

                stopSelf()
            }
        }

        sendBroadcast(Intent(Constants.CLASH_PROCESS_BROADCAST_ACTION).setPackage(packageName))
    }

    private fun reloadProfile() {
        executor.submit {
            if ( clash.getCurrentProcessStatus() != ProcessEvent.STARTED)
                return@submit

            val active = instance.profileService.queryActiveProfile()

            if (active == null) {
                sendError(ErrorEvent.Type.PROFILE_LOAD, "No profile activated")
                clash.stop()
                return@submit
            }

            Log.i("Loading profile ${active.file}")

            try {
                clash.loadProfile(active.file)

                notification.setProfile(active.name)

                events.performProfileReloadEvent(ProfileReloadEvent())
            } catch (e: Exception) {
                clash.stop()
                sendError(ErrorEvent.Type.PROFILE_LOAD, e.message)
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

    override fun onBandwidthEvent(event: BandwidthEvent?) {}
    override fun onLogEvent(event: LogEvent?) {}
    override fun onErrorEvent(event: ErrorEvent?) {}
    override fun asBinder(): IBinder = object : Binder() {
        override fun queryLocalInterface(descriptor: String): IInterface? {
            return this@ClashService
        }
    }

    private fun sendError(type: ErrorEvent.Type, message: String?) {
        events.performErrorEvent(ErrorEvent(type, message ?: "Unknown"))
    }
}